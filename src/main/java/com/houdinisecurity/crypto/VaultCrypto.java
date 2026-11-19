package com.houdinisecurity.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * KEK/DEK helpers for vault header and per-entry payloads.
 */
public final class VaultCrypto {

    public static final byte[] MAGIC = new byte[]{'H', 'O', 'U', 'D'};
    public static final short FORMAT_VERSION = 1;

    private final AesGcmCipher cipher;
    private final SecureRandom secureRandom;

    public VaultCrypto() {
        this(new AesGcmCipher(), new SecureRandom());
    }

    public VaultCrypto(AesGcmCipher cipher, SecureRandom secureRandom) {
        this.cipher = cipher;
        this.secureRandom = secureRandom;
    }

    public byte[] generateDek() {
        byte[] dek = new byte[AesGcmCipher.KEY_LENGTH];
        secureRandom.nextBytes(dek);
        return dek;
    }

    public byte[] generateSalt(int length) {
        if (length < KdfParameters.MIN_SALT_LENGTH) {
            throw new IllegalArgumentException("salt too short");
        }
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);
        return salt;
    }

    public static byte[] headerAad() {
        return ByteBuffer.allocate(6)
                .put(MAGIC)
                .putShort(FORMAT_VERSION)
                .array();
    }

    public static byte[] entryAad(String entryId) {
        return (entryId + "|" + FORMAT_VERSION).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] wrapDek(byte[] kek, byte[] dek) {
        return cipher.encrypt(kek, dek, headerAad());
    }

    public byte[] unwrapDek(byte[] kek, byte[] wrappedDek) {
        return cipher.decrypt(kek, wrappedDek, headerAad());
    }

    public byte[] encryptPayload(byte[] dek, String entryId, byte[] plaintextUtf8) {
        return cipher.encrypt(dek, plaintextUtf8, entryAad(entryId));
    }

    public byte[] decryptPayload(byte[] dek, String entryId, byte[] blob) {
        return cipher.decrypt(dek, blob, entryAad(entryId));
    }

    public AesGcmCipher cipher() {
        return cipher;
    }

    public static void wipeKey(byte[] key) {
        if (key != null) {
            Arrays.fill(key, (byte) 0);
        }
    }
}
