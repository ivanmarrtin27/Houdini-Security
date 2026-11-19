package com.houdinisecurity;

import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.service.AuditService;
import com.houdinisecurity.service.AutolockService;
import com.houdinisecurity.service.BackupService;
import com.houdinisecurity.service.ClipboardService;
import com.houdinisecurity.service.CsvImportService;
import com.houdinisecurity.service.PasswordGenerator;
import com.houdinisecurity.service.StrengthMeter;
import com.houdinisecurity.service.VaultService;
import com.houdinisecurity.ui.I18n;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Manual DI root. */
public final class AppContext {

    private final VaultCrypto vaultCrypto = new VaultCrypto();
    private final VaultService vaultService = new VaultService(vaultCrypto);
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();
    private final StrengthMeter strengthMeter = new StrengthMeter();
    private final AuditService auditService = new AuditService(strengthMeter);
    private final ClipboardService clipboardService = new ClipboardService();
    private final BackupService backupService = new BackupService(vaultCrypto);
    private final CsvImportService csvImportService = new CsvImportService();
    private final AutolockService autolockService;
    private final ExecutorService background = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "houdini-bg");
        t.setDaemon(true);
        return t;
    });
    private final I18n i18n = new I18n();

    public AppContext() {
        this.autolockService = new AutolockService(
                vaultService, clipboardService, () -> vaultService.settings().getLockTimeoutSec());
    }

    public VaultCrypto vaultCrypto() {
        return vaultCrypto;
    }

    public VaultService vaultService() {
        return vaultService;
    }

    public PasswordGenerator passwordGenerator() {
        return passwordGenerator;
    }

    public StrengthMeter strengthMeter() {
        return strengthMeter;
    }

    public AuditService auditService() {
        return auditService;
    }

    public ClipboardService clipboardService() {
        return clipboardService;
    }

    public BackupService backupService() {
        return backupService;
    }

    public CsvImportService csvImportService() {
        return csvImportService;
    }

    public AutolockService autolockService() {
        return autolockService;
    }

    public ExecutorService background() {
        return background;
    }

    public I18n i18n() {
        return i18n;
    }

    public void shutdown() {
        autolockService.shutdown();
        clipboardService.shutdown();
        background.shutdownNow();
        if (vaultService.isUnlocked()) {
            vaultService.lock();
        }
    }
}
