package com.houdinisecurity.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.ApiKeyEntry;
import com.houdinisecurity.domain.EntryType;
import com.houdinisecurity.domain.PasswordEntry;
import com.houdinisecurity.domain.SecureNote;
import com.houdinisecurity.domain.VaultEntry;

import java.nio.charset.StandardCharsets;

public final class EntryCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final VaultCrypto vaultCrypto;

    public EntryCodec(VaultCrypto vaultCrypto) {
        this.vaultCrypto = vaultCrypto;
    }

    public byte[] encode(byte[] dek, VaultEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("type", entry.getType().name());
        switch (entry) {
            case PasswordEntry p -> {
                json.addProperty("site", p.getSite());
                json.addProperty("username", p.getUsername());
                char[] pw = p.getPasswordCopy();
                char[] codes = p.getRecoveryCodesCopy();
                try {
                    json.addProperty("password", new String(pw));
                    json.addProperty("recoveryCodes", new String(codes));
                } finally {
                    SecureMemory.wipe(pw);
                    SecureMemory.wipe(codes);
                }
                json.addProperty("url", p.getUrl());
                json.addProperty("notes", p.getNotes());
            }
            case SecureNote n -> {
                json.addProperty("title", n.getTitle());
                json.addProperty("body", n.getBody());
            }
            case ApiKeyEntry a -> {
                json.addProperty("service", a.getService());
                char[] key = a.getApiKeyCopy();
                char[] secret = a.getApiSecretCopy();
                try {
                    json.addProperty("apiKey", new String(key));
                    json.addProperty("apiSecret", new String(secret));
                } finally {
                    SecureMemory.wipe(key);
                    SecureMemory.wipe(secret);
                }
                if (a.getExpiresAt() != null) {
                    json.addProperty("expiresAt", a.getExpiresAt());
                } else {
                    json.add("expiresAt", null);
                }
            }
            default -> throw new IllegalArgumentException("Unknown entry type");
        }
        String raw = GSON.toJson(json);
        byte[] utf8 = raw.getBytes(StandardCharsets.UTF_8);
        try {
            return vaultCrypto.encryptPayload(dek, entry.getId(), utf8);
        } finally {
            SecureMemory.wipe(utf8);
        }
    }

    public VaultEntry decode(byte[] dek, String id, EntryType type, String folderId, boolean favorite,
                             long createdAt, long updatedAt, byte[] ciphertext) {
        byte[] plain = vaultCrypto.decryptPayload(dek, id, ciphertext);
        try {
            String json = new String(plain, StandardCharsets.UTF_8);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            return switch (type) {
                case PASSWORD -> {
                    char[] pw = obj.has("password") && !obj.get("password").isJsonNull()
                            ? obj.get("password").getAsString().toCharArray() : new char[0];
                    // Soft migration: older vaults omit recoveryCodes → empty
                    char[] codes = obj.has("recoveryCodes") && !obj.get("recoveryCodes").isJsonNull()
                            ? obj.get("recoveryCodes").getAsString().toCharArray() : new char[0];
                    try {
                        yield new PasswordEntry(
                                id, folderId, favorite, createdAt, updatedAt,
                                text(obj, "site"), text(obj, "username"), pw,
                                text(obj, "url"), text(obj, "notes"), codes);
                    } finally {
                        SecureMemory.wipe(pw);
                        SecureMemory.wipe(codes);
                    }
                }
                case NOTE -> new SecureNote(
                        id, folderId, favorite, createdAt, updatedAt,
                        text(obj, "title"), text(obj, "body"));
                case API_KEY -> {
                    char[] key = obj.has("apiKey") && !obj.get("apiKey").isJsonNull()
                            ? obj.get("apiKey").getAsString().toCharArray() : new char[0];
                    char[] secret = obj.has("apiSecret") && !obj.get("apiSecret").isJsonNull()
                            ? obj.get("apiSecret").getAsString().toCharArray() : new char[0];
                    Long expires = obj.has("expiresAt") && !obj.get("expiresAt").isJsonNull()
                            ? obj.get("expiresAt").getAsLong() : null;
                    try {
                        yield new ApiKeyEntry(
                                id, folderId, favorite, createdAt, updatedAt,
                                text(obj, "service"), key, secret, expires);
                    } finally {
                        SecureMemory.wipe(key);
                        SecureMemory.wipe(secret);
                    }
                }
            };
        } finally {
            SecureMemory.wipe(plain);
        }
    }

    private static String text(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : "";
    }
}
