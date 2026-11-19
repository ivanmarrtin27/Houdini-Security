package com.houdinisecurity.domain;

import com.houdinisecurity.crypto.SecureMemory;

import java.util.Arrays;

public final class ApiKeyEntry extends VaultEntry {

    private String service;
    private char[] apiKey;
    private char[] apiSecret;
    private Long expiresAt;

    public ApiKeyEntry(String id, String folderId, boolean favorite, long createdAt, long updatedAt,
                       String service, char[] apiKey, char[] apiSecret, Long expiresAt) {
        super(id, folderId, favorite, createdAt, updatedAt);
        this.service = service == null ? "" : service;
        this.apiKey = apiKey == null ? new char[0] : Arrays.copyOf(apiKey, apiKey.length);
        this.apiSecret = apiSecret == null ? new char[0] : Arrays.copyOf(apiSecret, apiSecret.length);
        this.expiresAt = expiresAt;
    }

    public static ApiKeyEntry create(String service, char[] apiKey, char[] apiSecret, Long expiresAt) {
        long now = System.currentTimeMillis();
        return new ApiKeyEntry(null, null, false, now, now, service, apiKey, apiSecret, expiresAt);
    }

    @Override
    public EntryType getType() {
        return EntryType.API_KEY;
    }

    @Override
    public String displayTitle() {
        return service.isBlank() ? "(api key)" : service;
    }

    @Override
    public String searchableText() {
        return service.toLowerCase();
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service == null ? "" : service;
    }

    public char[] getApiKeyCopy() {
        return Arrays.copyOf(apiKey, apiKey.length);
    }

    public void setApiKey(char[] apiKey) {
        SecureMemory.wipe(this.apiKey);
        this.apiKey = apiKey == null ? new char[0] : Arrays.copyOf(apiKey, apiKey.length);
    }

    public char[] getApiSecretCopy() {
        return Arrays.copyOf(apiSecret, apiSecret.length);
    }

    public void setApiSecret(char[] apiSecret) {
        SecureMemory.wipe(this.apiSecret);
        this.apiSecret = apiSecret == null ? new char[0] : Arrays.copyOf(apiSecret, apiSecret.length);
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void wipeSecrets() {
        SecureMemory.wipe(apiKey);
        SecureMemory.wipe(apiSecret);
        apiKey = new char[0];
        apiSecret = new char[0];
    }

    @Override
    public String toString() {
        return "ApiKeyEntry{id=" + getId() + ", service=" + service + "}";
    }
}
