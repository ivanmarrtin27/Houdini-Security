package com.houdinisecurity.service;

import com.houdinisecurity.crypto.KdfAlgorithm;
import com.houdinisecurity.crypto.KdfParameters;
import com.houdinisecurity.crypto.KeyDerivation;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.CorruptedVaultException;
import com.houdinisecurity.domain.Folder;
import com.houdinisecurity.domain.InvalidMasterPasswordException;
import com.houdinisecurity.domain.Tag;
import com.houdinisecurity.domain.VaultEntry;
import com.houdinisecurity.domain.VaultLockedException;
import com.houdinisecurity.storage.SqliteVaultRepository;
import com.houdinisecurity.storage.VaultHeader;
import com.houdinisecurity.storage.VaultPaths;
import com.houdinisecurity.storage.VaultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class VaultService {

    private static final Logger log = LoggerFactory.getLogger(VaultService.class);

    private final VaultCrypto vaultCrypto;
    private final RetryBackoff backoff = new RetryBackoff();
    private final AppSettings settings = new AppSettings();
    private final List<Consumer<Boolean>> lockListeners = new CopyOnWriteArrayList<>();

    private Path headerPath;
    private VaultRepository repository;
    private byte[] dek;
    private final List<VaultEntry> cache = new ArrayList<>();
    private List<Folder> folders = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    public VaultService(VaultCrypto vaultCrypto) {
        this.vaultCrypto = vaultCrypto;
    }

    public AppSettings settings() {
        return settings;
    }

    public void addLockListener(Consumer<Boolean> listener) {
        lockListeners.add(listener);
    }

    public boolean isUnlocked() {
        return dek != null;
    }

    public Path currentHeaderPath() {
        return headerPath;
    }

    public void createVault(Path headerPath, char[] masterPassword, KdfAlgorithm algorithm) throws IOException {
        Objects.requireNonNull(headerPath);
        if (Files.exists(headerPath)) {
            throw new IllegalArgumentException("Vault already exists");
        }
        Path dbPath = VaultPaths.dbPath(headerPath);
        byte[] salt = vaultCrypto.generateSalt(16);
        KdfParameters params = algorithm == KdfAlgorithm.PBKDF2_HMAC_SHA256
                ? KdfParameters.pbkdf2Defaults(salt)
                : KdfParameters.argon2Defaults(salt);
        byte[] kek = null;
        byte[] newDek = null;
        byte[] wrapped = null;
        try {
            kek = KeyDerivation.deriveKek(masterPassword, params);
            newDek = vaultCrypto.generateDek();
            wrapped = vaultCrypto.wrapDek(kek, newDek);
            VaultHeader header = new VaultHeader(VaultCrypto.FORMAT_VERSION, params, wrapped);
            Files.createDirectories(headerPath.getParent() == null ? Path.of(".") : headerPath.getParent());
            header.writeAtomic(headerPath);
            VaultRepository repo = new SqliteVaultRepository(dbPath, vaultCrypto);
            repo.initSchema();
            this.headerPath = headerPath;
            this.repository = repo;
            this.dek = newDek;
            newDek = null;
            this.cache.clear();
            this.folders = new ArrayList<>();
            this.tags = new ArrayList<>();
            loadSettingsFromRepo();
            notifyLock(false);
            log.info("Vault created at {}", headerPath.getFileName());
        } finally {
            SecureMemory.wipe(kek);
            SecureMemory.wipe(newDek);
            SecureMemory.wipe(wrapped);
            SecureMemory.wipe(salt);
        }
    }

    public void unlock(Path headerPath, char[] masterPassword) throws IOException, InterruptedException {
        backoff.awaitIfNeeded();
        VaultHeader header = VaultHeader.read(headerPath);
        byte[] kek = null;
        try {
            kek = KeyDerivation.deriveKek(masterPassword, header.kdfParameters());
            byte[] unwrapped = vaultCrypto.unwrapDek(kek, header.wrappedDekCopy());
            Path dbPath = VaultPaths.dbPath(headerPath);
            if (!Files.exists(dbPath)) {
                throw new CorruptedVaultException("Missing vault database");
            }
            closeQuietly();
            this.headerPath = headerPath;
            this.repository = new SqliteVaultRepository(dbPath, vaultCrypto);
            this.dek = unwrapped;
            reloadCache();
            loadSettingsFromRepo();
            backoff.onSuccess();
            notifyLock(false);
            log.info("Vault unlocked");
        } catch (IllegalStateException e) {
            backoff.onFailure();
            throw new InvalidMasterPasswordException("Invalid master password", e);
        } finally {
            SecureMemory.wipe(kek);
            header.wipe();
        }
    }

    public void lock() {
        wipeCacheSecrets();
        cache.clear();
        folders = new ArrayList<>();
        tags = new ArrayList<>();
        VaultCrypto.wipeKey(dek);
        dek = null;
        closeQuietly();
        notifyLock(true);
        log.info("Vault locked");
    }

    public void changeMasterPassword(char[] currentPassword, char[] newPassword, KdfAlgorithm algorithm)
            throws IOException, InterruptedException {
        ensureUnlocked();
        backoff.awaitIfNeeded();
        VaultHeader header = VaultHeader.read(headerPath);
        byte[] currentKek = null;
        byte[] newKek = null;
        byte[] wrapped = null;
        byte[] salt = null;
        try {
            currentKek = KeyDerivation.deriveKek(currentPassword, header.kdfParameters());
            try {
                byte[] check = vaultCrypto.unwrapDek(currentKek, header.wrappedDekCopy());
                SecureMemory.wipe(check);
            } catch (IllegalStateException e) {
                backoff.onFailure();
                throw new InvalidMasterPasswordException("Invalid master password", e);
            }
            Path bak = VaultPaths.backupPath(headerPath);
            Files.copy(headerPath, bak, StandardCopyOption.REPLACE_EXISTING);
            salt = vaultCrypto.generateSalt(16);
            KdfParameters params = algorithm == KdfAlgorithm.PBKDF2_HMAC_SHA256
                    ? KdfParameters.pbkdf2Defaults(salt)
                    : KdfParameters.argon2Defaults(salt);
            newKek = KeyDerivation.deriveKek(newPassword, params);
            wrapped = vaultCrypto.wrapDek(newKek, dek);
            VaultHeader newHeader = new VaultHeader(VaultCrypto.FORMAT_VERSION, params, wrapped);
            newHeader.writeAtomic(headerPath);
            backoff.onSuccess();
            log.info("Master password changed (DEK re-wrapped)");
        } finally {
            SecureMemory.wipe(currentKek);
            SecureMemory.wipe(newKek);
            SecureMemory.wipe(wrapped);
            SecureMemory.wipe(salt);
            header.wipe();
        }
    }

    public List<VaultEntry> listEntries() {
        ensureUnlocked();
        return List.copyOf(cache);
    }

    public List<VaultEntry> search(String query, String folderId, String tagId, boolean favoritesOnly) {
        ensureUnlocked();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return cache.stream()
                .filter(e -> !favoritesOnly || e.isFavorite())
                .filter(e -> folderId == null || folderId.equals(e.getFolderId()))
                .filter(e -> tagId == null || e.getTagIds().contains(tagId))
                .filter(e -> q.isEmpty() || e.searchableText().contains(q) || e.displayTitle().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    public void saveEntry(VaultEntry entry) {
        ensureUnlocked();
        entry.touch(System.currentTimeMillis());
        repository.saveEntry(dek, entry);
        cache.removeIf(e -> e.getId().equals(entry.getId()));
        cache.add(entry);
    }

    public void deleteEntry(String entryId) {
        ensureUnlocked();
        repository.deleteEntry(entryId);
        cache.stream().filter(e -> e.getId().equals(entryId)).forEach(this::wipeEntry);
        cache.removeIf(e -> e.getId().equals(entryId));
    }

    public List<Folder> listFolders() {
        ensureUnlocked();
        return List.copyOf(folders);
    }

    public void saveFolder(Folder folder) {
        ensureUnlocked();
        repository.saveFolder(folder);
        folders.removeIf(f -> f.getId().equals(folder.getId()));
        folders.add(folder);
    }

    public void deleteFolder(String folderId) {
        ensureUnlocked();
        repository.deleteFolder(folderId);
        folders.removeIf(f -> f.getId().equals(folderId));
    }

    public List<Tag> listTags() {
        ensureUnlocked();
        return List.copyOf(tags);
    }

    public void saveTag(Tag tag) {
        ensureUnlocked();
        repository.saveTag(tag);
        tags.removeIf(t -> t.getId().equals(tag.getId()));
        tags.add(tag);
    }

    public void deleteTag(String tagId) {
        ensureUnlocked();
        repository.deleteTag(tagId);
        tags.removeIf(t -> t.getId().equals(tagId));
    }

    public void persistSettings() {
        ensureUnlocked();
        repository.putSetting(AppSettings.KEY_THEME, settings.getTheme());
        repository.putSetting(AppSettings.KEY_LOCK_TIMEOUT_SEC, String.valueOf(settings.getLockTimeoutSec()));
        repository.putSetting(AppSettings.KEY_CLIPBOARD_TIMEOUT_SEC, String.valueOf(settings.getClipboardTimeoutSec()));
        repository.putSetting(AppSettings.KEY_AUDIT_OLD_DAYS, String.valueOf(settings.getAuditOldDays()));
        repository.putSetting(AppSettings.KEY_API_EXPIRY_WARN_DAYS, String.valueOf(settings.getApiExpiryWarnDays()));
        repository.putSetting(AppSettings.KEY_LOCALE, settings.getLocale());
    }

    public byte[] dekCopyForBackup() {
        ensureUnlocked();
        return dek.clone();
    }

    public VaultRepository repository() {
        ensureUnlocked();
        return repository;
    }

    private void reloadCache() {
        cache.clear();
        cache.addAll(repository.loadAllEntries(dek));
        folders = new ArrayList<>(repository.listFolders());
        tags = new ArrayList<>(repository.listTags());
    }

    private void loadSettingsFromRepo() {
        repository.getSetting(AppSettings.KEY_THEME).ifPresent(settings::setTheme);
        repository.getSetting(AppSettings.KEY_LOCK_TIMEOUT_SEC).ifPresent(v -> settings.setLockTimeoutSec(Integer.parseInt(v)));
        repository.getSetting(AppSettings.KEY_CLIPBOARD_TIMEOUT_SEC).ifPresent(v -> settings.setClipboardTimeoutSec(Integer.parseInt(v)));
        repository.getSetting(AppSettings.KEY_AUDIT_OLD_DAYS).ifPresent(v -> settings.setAuditOldDays(Integer.parseInt(v)));
        repository.getSetting(AppSettings.KEY_API_EXPIRY_WARN_DAYS).ifPresent(v -> settings.setApiExpiryWarnDays(Integer.parseInt(v)));
        repository.getSetting(AppSettings.KEY_LOCALE).ifPresent(settings::setLocale);
    }

    private void ensureUnlocked() {
        if (dek == null || repository == null) {
            throw new VaultLockedException("Vault is locked");
        }
    }

    private void wipeCacheSecrets() {
        for (VaultEntry e : cache) {
            wipeEntry(e);
        }
    }

    private void wipeEntry(VaultEntry e) {
        if (e instanceof com.houdinisecurity.domain.PasswordEntry p) {
            p.wipeSecrets();
        } else if (e instanceof com.houdinisecurity.domain.SecureNote n) {
            n.wipeSecrets();
        } else if (e instanceof com.houdinisecurity.domain.ApiKeyEntry a) {
            a.wipeSecrets();
        }
    }

    private void closeQuietly() {
        if (repository != null) {
            try {
                repository.close();
            } catch (Exception ignored) {
                // ignore
            }
            repository = null;
        }
    }

    private void notifyLock(boolean locked) {
        for (Consumer<Boolean> l : lockListeners) {
            try {
                l.accept(locked);
            } catch (Exception ignored) {
                // ignore listener errors
            }
        }
    }
}
