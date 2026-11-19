package com.houdinisecurity.service;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Copies secrets and clears the system clipboard after N seconds
 * only if content still matches what this app placed.
 */
public final class ClipboardService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "houdini-clipboard");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<String> lastCopied = new AtomicReference<>();
    private ScheduledFuture<?> clearTask;

    public void copy(String value, int timeoutSec) {
        if (value == null) {
            return;
        }
        Platform.runLater(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
            lastCopied.set(value);
            scheduleClear(timeoutSec);
        });
    }

    public void copyChars(char[] value, int timeoutSec) {
        if (value == null) {
            return;
        }
        copy(new String(value), timeoutSec);
    }

    private synchronized void scheduleClear(int timeoutSec) {
        if (clearTask != null) {
            clearTask.cancel(false);
        }
        clearTask = scheduler.schedule(() -> Platform.runLater(this::clearIfOurs), timeoutSec, TimeUnit.SECONDS);
    }

    public void clearIfOurs() {
        String expected = lastCopied.get();
        if (expected == null) {
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String current = clipboard.getString();
        if (expected.equals(current)) {
            ClipboardContent empty = new ClipboardContent();
            empty.putString("");
            clipboard.setContent(empty);
        }
        lastCopied.set(null);
    }

    public void shutdown() {
        clearIfOurs();
        scheduler.shutdownNow();
    }
}
