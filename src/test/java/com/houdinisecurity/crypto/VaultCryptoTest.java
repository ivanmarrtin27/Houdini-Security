package com.houdinisecurity.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultCryptoTest {

    @Test
    void wrapUnwrapDek() {
        VaultCrypto vc = new VaultCrypto();
        byte[] kek = new byte[32];
        byte[] dek = vc.generateDek();
        try {
            byte[] wrapped = vc.wrapDek(kek, dek);
            byte[] unwrapped = vc.unwrapDek(kek, wrapped);
            assertArrayEquals(dek, unwrapped);
            SecureMemory.wipe(unwrapped);
            SecureMemory.wipe(wrapped);
        } finally {
            SecureMemory.wipe(dek);
        }
    }

    @Test
    void badKekFailsAuth() {
        VaultCrypto vc = new VaultCrypto();
        byte[] kek = new byte[32];
        byte[] bad = new byte[32];
        bad[0] = 1;
        byte[] dek = vc.generateDek();
        try {
            byte[] wrapped = vc.wrapDek(kek, dek);
            assertThrows(IllegalStateException.class, () -> vc.unwrapDek(bad, wrapped));
            SecureMemory.wipe(wrapped);
        } finally {
            SecureMemory.wipe(dek);
        }
    }

    @Test
    void entryPayloadRoundTrip() {
        VaultCrypto vc = new VaultCrypto();
        byte[] dek = vc.generateDek();
        try {
            byte[] plain = "{\"site\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            byte[] blob = vc.encryptPayload(dek, "id-1", plain);
            byte[] out = vc.decryptPayload(dek, "id-1", blob);
            assertArrayEquals(plain, out);
            SecureMemory.wipe(plain);
            SecureMemory.wipe(out);
            SecureMemory.wipe(blob);
        } finally {
            SecureMemory.wipe(dek);
        }
    }
}
