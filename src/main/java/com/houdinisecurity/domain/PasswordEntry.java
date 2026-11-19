package com.houdinisecurity.domain;

import com.houdinisecurity.crypto.SecureMemory;

import java.util.Arrays;

public final class PasswordEntry extends VaultEntry {

    private String site;
    private String username;
    private char[] password;
    private String url;
    private String notes;
    /** Backup / 2FA recovery codes; stored encrypted inside the entry payload. */
    private char[] recoveryCodes;

    public PasswordEntry(String id, String folderId, boolean favorite, long createdAt, long updatedAt,
                         String site, String username, char[] password, String url, String notes) {
        this(id, folderId, favorite, createdAt, updatedAt, site, username, password, url, notes, null);
    }

    public PasswordEntry(String id, String folderId, boolean favorite, long createdAt, long updatedAt,
                         String site, String username, char[] password, String url, String notes,
                         char[] recoveryCodes) {
        super(id, folderId, favorite, createdAt, updatedAt);
        this.site = site == null ? "" : site;
        this.username = username == null ? "" : username;
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        this.url = url == null ? "" : url;
        this.notes = notes == null ? "" : notes;
        this.recoveryCodes = recoveryCodes == null
                ? new char[0]
                : Arrays.copyOf(recoveryCodes, recoveryCodes.length);
    }

    public static PasswordEntry create(String site, String username, char[] password, String url, String notes) {
        return create(site, username, password, url, notes, null);
    }

    public static PasswordEntry create(String site, String username, char[] password, String url, String notes,
                                       char[] recoveryCodes) {
        long now = System.currentTimeMillis();
        return new PasswordEntry(null, null, false, now, now, site, username, password, url, notes, recoveryCodes);
    }

    @Override
    public EntryType getType() {
        return EntryType.PASSWORD;
    }

    @Override
    public String displayTitle() {
        return site.isBlank() ? username : site;
    }

    @Override
    public String searchableText() {
        return (site + " " + username + " " + url + " " + notes).toLowerCase();
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site == null ? "" : site;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public char[] getPasswordCopy() {
        return Arrays.copyOf(password, password.length);
    }

    public void setPassword(char[] password) {
        SecureMemory.wipe(this.password);
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public char[] getRecoveryCodesCopy() {
        return Arrays.copyOf(recoveryCodes, recoveryCodes.length);
    }

    public void setRecoveryCodes(char[] recoveryCodes) {
        SecureMemory.wipe(this.recoveryCodes);
        this.recoveryCodes = recoveryCodes == null
                ? new char[0]
                : Arrays.copyOf(recoveryCodes, recoveryCodes.length);
    }

    public boolean hasRecoveryCodes() {
        return recoveryCodes.length > 0;
    }

    public void wipeSecrets() {
        SecureMemory.wipe(password);
        password = new char[0];
        SecureMemory.wipe(recoveryCodes);
        recoveryCodes = new char[0];
    }

    @Override
    public String toString() {
        return "PasswordEntry{id=" + getId() + ", site=" + site + "}";
    }
}
