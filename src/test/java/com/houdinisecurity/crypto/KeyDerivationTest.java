package com.houdinisecurity.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KeyDerivationTest {

    @Test
    void pbkdf2DeterministicKnownParams() {
        byte[] salt = "houdini-salt-16b".getBytes(StandardCharsets.UTF_8);
        KdfParameters params = KdfParameters.pbkdf2Defaults(salt);
        char[] password = "test-password-xyz".toCharArray();
        byte[] a = KeyDerivation.deriveAndCopy(password, params);
        byte[] b = KeyDerivation.deriveAndCopy(password, params);
        try {
            assertEquals(32, a.length);
            assertFalse(Arrays.equals(a, new byte[32]));
            org.junit.jupiter.api.Assertions.assertArrayEquals(a, b);
        } finally {
            SecureMemory.wipe(a);
            SecureMemory.wipe(b);
            SecureMemory.wipe(password);
        }
    }

    @Test
    void argon2idProducesKey() {
        byte[] salt = new byte[16];
        Arrays.fill(salt, (byte) 7);
        KdfParameters params = KdfParameters.argon2Defaults(salt);
        char[] password = "argon2-master!!".toCharArray();
        byte[] key = KeyDerivation.deriveAndCopy(password, params);
        try {
            assertEquals(32, key.length);
            assertFalse(Arrays.equals(key, new byte[32]));
        } finally {
            SecureMemory.wipe(key);
            SecureMemory.wipe(password);
        }
    }
}
