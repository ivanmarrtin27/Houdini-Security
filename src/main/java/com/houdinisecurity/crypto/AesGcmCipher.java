package com.houdinisecurity.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM with 96-bit nonce and 128-bit tag.
 * Wire format: nonce (12) || ciphertext || tag (16).
 */
public final class AesGcmCipher {

    public static final int NONCE_LENGTH = 12;
    public static final int TAG_BITS = 128;
    public static final int TAG_LENGTH = 16;
    public static final int KEY_LENGTH = 32;

    private final SecureRandom secureRandom;

    public AesGcmCipher() {
        this(new SecureRandom());
    }

    public AesGcmCipher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public byte[] encrypt(byte[] key, byte[] plaintext, byte[] aad) {
        validateKey(key);
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, nonce));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[NONCE_LENGTH + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LENGTH);
            System.arraycopy(ct, 0, out, NONCE_LENGTH, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        } finally {
            SecureMemory.wipe(nonce);
        }
    }

    public byte[] decrypt(byte[] key, byte[] blob, byte[] aad) {
        validateKey(key);
        if (blob == null || blob.length < NONCE_LENGTH + TAG_LENGTH + 1) {
            throw new IllegalArgumentException("Ciphertext too short");
        }
        byte[] nonce = Arrays.copyOfRange(blob, 0, NONCE_LENGTH);
        byte[] ct = Arrays.copyOfRange(blob, NONCE_LENGTH, blob.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, nonce));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            return cipher.doFinal(ct);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decrypt/authenticate failed", e);
        } finally {
            SecureMemory.wipe(nonce);
            SecureMemory.wipe(ct);
        }
    }

    public byte[] newNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    private static void validateKey(byte[] key) {
        if (key == null || key.length != KEY_LENGTH) {
            throw new IllegalArgumentException("AES-256 key required");
        }
    }
}
