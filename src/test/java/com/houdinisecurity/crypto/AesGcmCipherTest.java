package com.houdinisecurity.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesGcmCipherTest {

    @Test
    void roundTrip() {
        AesGcmCipher cipher = new AesGcmCipher();
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        byte[] plain = "hola-secreto".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "aad-v1".getBytes(StandardCharsets.UTF_8);
        byte[] blob = cipher.encrypt(key, plain, aad);
        byte[] out = cipher.decrypt(key, blob, aad);
        assertArrayEquals(plain, out);
    }

    @Test
    void tamperFails() {
        AesGcmCipher cipher = new AesGcmCipher();
        byte[] key = new byte[32];
        byte[] blob = cipher.encrypt(key, "data".getBytes(StandardCharsets.UTF_8), null);
        blob[blob.length - 1] ^= 0x01;
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(key, blob, null));
    }

    @Test
    void nonceUniqueness() {
        AesGcmCipher cipher = new AesGcmCipher();
        Set<String> nonces = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            byte[] n = cipher.newNonce();
            assertEquals(12, n.length);
            String hex = java.util.HexFormat.of().formatHex(n);
            assertTrue(nonces.add(hex), "duplicate nonce");
        }
    }
}
