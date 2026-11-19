package com.houdinisecurity.service;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public final class AutolockService {

    private final VaultService vaultService;
    private final ClipboardService clipboardService;
    private final IntSupplier timeoutSeconds;
    private final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "houdini-autolock");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> watcher;
    private Runnable onLockedUi;

    public AutolockService(VaultService vaultService, ClipboardService clipboardService, IntSupplier timeoutSeconds) {
        this.vaultService = vaultService;
        this.clipboardService = clipboardService;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setOnLockedUi(Runnable onLockedUi) {
        this.onLockedUi = onLockedUi;
    }

    public void start() {
        stop();
        watcher = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (watcher != null) {
            watcher.cancel(false);
            watcher = null;
        }
    }

    public void bumpActivity() {
        lastActivity.set(System.currentTimeMillis());
    }

    public void lockNow() {
        if (!vaultService.isUnlocked()) {
            return;
        }
        clipboardService.clearIfOurs();
        vaultService.lock();
        if (onLockedUi != null) {
            Platform.runLater(onLockedUi);
        }
    }

    private void tick() {
        if (!vaultService.isUnlocked()) {
            return;
        }
        long idleMs = System.currentTimeMillis() - lastActivity.get();
        if (idleMs >= timeoutSeconds.getAsInt() * 1000L) {
            lockNow();
        }
    }

    public void shutdown() {
        stop();
        scheduler.shutdownNow();
    }
}
