package com.houdinisecurity.service;

public final class RetryBackoff {

    private int failures;
    private long nextAllowedAt;

    public synchronized void onFailure() {
        failures++;
        long delayMs = Math.min(30_000L, 250L * (1L << Math.min(failures, 7)));
        nextAllowedAt = System.currentTimeMillis() + delayMs;
    }

    public synchronized void onSuccess() {
        failures = 0;
        nextAllowedAt = 0;
    }

    public synchronized void awaitIfNeeded() throws InterruptedException {
        long wait = nextAllowedAt - System.currentTimeMillis();
        if (wait > 0) {
            Thread.sleep(wait);
        }
    }

    public synchronized int failures() {
        return failures;
    }
}
