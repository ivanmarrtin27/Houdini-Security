package com.houdinisecurity.service;

import com.houdinisecurity.crypto.AesGcmCipher;
import com.houdinisecurity.crypto.KdfParameters;
import com.houdinisecurity.crypto.KeyDerivation;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.CorruptedVaultException;
import com.houdinisecurity.storage.VaultHeader;
import com.houdinisecurity.storage.VaultPaths;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Encrypted backup container magic HBAK v1:
 * magic(4) + version(2) + kdf+salt+wrappedBackupKey + enc(headerBytes) + enc(dbBytes)
 * For simplicity: passphrase-derived key encrypts a zip-like concatenation.
 */
public final class BackupService {

    private static final byte[] MAGIC = new byte[]{'H', 'B', 'A', 'K'};
    private static final short VERSION = 1;

    private final VaultCrypto vaultCrypto;
    private final AesGcmCipher cipher;

    public BackupService(VaultCrypto vaultCrypto) {
        this.vaultCrypto = vaultCrypto;
        this.cipher = vaultCrypto.cipher();
    }

    public void exportBackup(Path headerPath, char[] backupPassphrase, Path output) throws IOException {
        Path dbPath = VaultPaths.dbPath(headerPath);
        byte[] headerBytes = Files.readAllBytes(headerPath);
        byte[] dbBytes = Files.readAllBytes(dbPath);
        byte[] salt = vaultCrypto.generateSalt(16);
        KdfParameters params = KdfParameters.argon2Defaults(salt);
        byte[] key = null;
        try {
            key = KeyDerivation.deriveKek(backupPassphrase, params);
            byte[] aad = ByteBuffer.allocate(6).put(MAGIC).putShort(VERSION).array();
            byte[] payload = ByteBuffer.allocate(4 + headerBytes.length + 4 + dbBytes.length)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(headerBytes.length)
                    .put(headerBytes)
                    .putInt(dbBytes.length)
                    .put(dbBytes)
                    .array();
            byte[] enc = cipher.encrypt(key, payload, aad);
            ByteBuffer out = ByteBuffer.allocate(4 + 2 + 1 + 1 + 4 + 4 + 4 + 2 + salt.length + enc.length)
                    .order(ByteOrder.LITTLE_ENDIAN);
            out.put(MAGIC);
            out.putShort(VERSION);
            out.put(params.algorithm().id());
            out.put((byte) 0);
            out.putInt(params.memoryKiB());
            out.putInt(params.iterations());
            out.putInt(params.parallelism());
            out.putShort((short) salt.length);
            out.put(salt);
            out.put(enc);
            Files.write(output, out.array());
            SecureMemory.wipe(payload);
            SecureMemory.wipe(enc);
        } finally {
            SecureMemory.wipe(key);
            SecureMemory.wipe(salt);
            SecureMemory.wipe(headerBytes);
            SecureMemory.wipe(dbBytes);
        }
    }

    public void importBackup(Path backupFile, char[] backupPassphrase, Path targetHeaderPath) throws IOException {
        byte[] data = Files.readAllBytes(backupFile);
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[4];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new CorruptedVaultException("Invalid backup magic");
        }
        short version = buf.getShort();
        if (version != VERSION) {
            throw new CorruptedVaultException("Unsupported backup version");
        }
        byte kdfId = buf.get();
        buf.get();
        int memory = buf.getInt();
        int iterations = buf.getInt();
        int parallelism = buf.getInt();
        int saltLen = Short.toUnsignedInt(buf.getShort());
        byte[] salt = new byte[saltLen];
        buf.get(salt);
        byte[] enc = new byte[buf.remaining()];
        buf.get(enc);
        KdfParameters params = new KdfParameters(
                com.houdinisecurity.crypto.KdfAlgorithm.fromId(kdfId), memory, iterations, parallelism, salt);
        byte[] key = null;
        try {
            key = KeyDerivation.deriveKek(backupPassphrase, params);
            byte[] aad = ByteBuffer.allocate(6).put(MAGIC).putShort(VERSION).array();
            byte[] payload = cipher.decrypt(key, enc, aad);
            ByteBuffer p = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            int headerLen = p.getInt();
            byte[] headerBytes = new byte[headerLen];
            p.get(headerBytes);
            int dbLen = p.getInt();
            byte[] dbBytes = new byte[dbLen];
            p.get(dbBytes);
            Path parent = targetHeaderPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(targetHeaderPath, headerBytes);
            Files.write(VaultPaths.dbPath(targetHeaderPath), dbBytes);
            SecureMemory.wipe(payload);
            SecureMemory.wipe(headerBytes);
            SecureMemory.wipe(dbBytes);
        } catch (IllegalStateException e) {
            throw new CorruptedVaultException("Backup decrypt failed", e);
        } finally {
            SecureMemory.wipe(key);
            SecureMemory.wipe(salt);
            SecureMemory.wipe(enc);
        }
    }
}
