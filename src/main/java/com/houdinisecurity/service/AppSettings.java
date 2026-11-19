package com.houdinisecurity.service;

public final class AppSettings {

    public static final String KEY_THEME = "theme";
    public static final String KEY_LOCK_TIMEOUT_SEC = "lockTimeoutSec";
    public static final String KEY_CLIPBOARD_TIMEOUT_SEC = "clipboardTimeoutSec";
    public static final String KEY_AUDIT_OLD_DAYS = "auditOldDays";
    public static final String KEY_API_EXPIRY_WARN_DAYS = "apiExpiryWarnDays";
    public static final String KEY_LOCALE = "locale";

    public static final int DEFAULT_LOCK_TIMEOUT_SEC = 300;
    public static final int DEFAULT_CLIPBOARD_TIMEOUT_SEC = 20;
    public static final int DEFAULT_AUDIT_OLD_DAYS = 365;
    public static final int DEFAULT_API_EXPIRY_WARN_DAYS = 30;

    private int lockTimeoutSec = DEFAULT_LOCK_TIMEOUT_SEC;
    private int clipboardTimeoutSec = DEFAULT_CLIPBOARD_TIMEOUT_SEC;
    private int auditOldDays = DEFAULT_AUDIT_OLD_DAYS;
    private int apiExpiryWarnDays = DEFAULT_API_EXPIRY_WARN_DAYS;
    private String theme = "dark";
    private String locale = "es";

    public int getLockTimeoutSec() {
        return lockTimeoutSec;
    }

    public void setLockTimeoutSec(int lockTimeoutSec) {
        this.lockTimeoutSec = Math.max(30, lockTimeoutSec);
    }

    public int getClipboardTimeoutSec() {
        return clipboardTimeoutSec;
    }

    public void setClipboardTimeoutSec(int clipboardTimeoutSec) {
        this.clipboardTimeoutSec = Math.max(5, clipboardTimeoutSec);
    }

    public int getAuditOldDays() {
        return auditOldDays;
    }

    public void setAuditOldDays(int auditOldDays) {
        this.auditOldDays = Math.max(1, auditOldDays);
    }

    public int getApiExpiryWarnDays() {
        return apiExpiryWarnDays;
    }

    public void setApiExpiryWarnDays(int apiExpiryWarnDays) {
        this.apiExpiryWarnDays = Math.max(1, apiExpiryWarnDays);
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme == null ? "dark" : theme;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale == null ? "es" : locale;
    }
}
