package com.houdinisecurity.storage;

import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.EntryType;
import com.houdinisecurity.domain.PasswordEntry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntryCodecTest {

    @Test
    void roundTripPasswordWithRecoveryCodes() {
        VaultCrypto crypto = new VaultCrypto();
        EntryCodec codec = new EntryCodec(crypto);
        byte[] dek = crypto.generateDek();
        try {
            char[] pw = "S3cret!".toCharArray();
            char[] codes = "AAAA-1111\nBBBB-2222".toCharArray();
            long now = System.currentTimeMillis();
            PasswordEntry original;
            try {
                original = new PasswordEntry(
                        "entry-1", null, false, now, now,
                        "GitHub", "me@ex.com", pw, "https://github.com", "notas", codes);
            } finally {
                SecureMemory.wipe(pw);
                SecureMemory.wipe(codes);
            }

            byte[] blob = codec.encode(dek, original);
            PasswordEntry loaded = (PasswordEntry) codec.decode(
                    dek, "entry-1", EntryType.PASSWORD, null, false,
                    original.getCreatedAt(), original.getUpdatedAt(), blob);

            assertEquals("GitHub", loaded.getSite());
            assertEquals("me@ex.com", loaded.getUsername());
            assertEquals("https://github.com", loaded.getUrl());
            assertEquals("notas", loaded.getNotes());

            char[] loadedPw = loaded.getPasswordCopy();
            char[] loadedCodes = loaded.getRecoveryCodesCopy();
            try {
                assertEquals("S3cret!", new String(loadedPw));
                assertEquals("AAAA-1111\nBBBB-2222", new String(loadedCodes));
            } finally {
                SecureMemory.wipe(loadedPw);
                SecureMemory.wipe(loadedCodes);
            }
            loaded.wipeSecrets();
            original.wipeSecrets();
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    @Test
    void missingRecoveryCodesFieldDecodesAsEmpty() {
        VaultCrypto crypto = new VaultCrypto();
        EntryCodec codec = new EntryCodec(crypto);
        byte[] dek = crypto.generateDek();
        try {
            String legacyJson = "{\"type\":\"PASSWORD\",\"site\":\"old\",\"username\":\"u\","
                    + "\"password\":\"p\",\"url\":\"\",\"notes\":\"\"}";
            byte[] plain = legacyJson.getBytes(StandardCharsets.UTF_8);
            byte[] blob;
            try {
                blob = crypto.encryptPayload(dek, "legacy-1", plain);
            } finally {
                SecureMemory.wipe(plain);
            }

            PasswordEntry loaded = (PasswordEntry) codec.decode(
                    dek, "legacy-1", EntryType.PASSWORD, null, false, 1L, 1L, blob);

            assertEquals("old", loaded.getSite());
            assertFalse(loaded.hasRecoveryCodes());
            char[] codes = loaded.getRecoveryCodesCopy();
            try {
                assertEquals(0, codes.length);
            } finally {
                SecureMemory.wipe(codes);
            }
            loaded.wipeSecrets();
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }
}
