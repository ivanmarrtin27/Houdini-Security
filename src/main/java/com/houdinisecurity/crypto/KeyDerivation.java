package com.houdinisecurity.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.Security;
import java.util.Arrays;

public final class KeyDerivation {

    public static final int KEY_LENGTH_BYTES = 32;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private KeyDerivation() {
    }

    public static byte[] deriveKek(char[] masterPassword, KdfParameters params) {
        byte[] passwordBytes = null;
        try {
            passwordBytes = SecureMemory.toUtf8Bytes(masterPassword);
            return switch (params.algorithm()) {
                case ARGON2ID -> argon2id(passwordBytes, params);
                case PBKDF2_HMAC_SHA256 -> pbkdf2(masterPassword, params);
            };
        } finally {
            SecureMemory.wipe(passwordBytes);
        }
    }

    private static byte[] argon2id(byte[] passwordBytes, KdfParameters params) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(params.saltCopy())
                .withMemoryAsKB(params.memoryKiB())
                .withIterations(params.iterations())
                .withParallelism(params.parallelism());
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());
        byte[] out = new byte[KEY_LENGTH_BYTES];
        gen.generateBytes(passwordBytes, out);
        return out;
    }

    private static byte[] pbkdf2(char[] password, KdfParameters params) {
        PBEKeySpec spec = null;
        try {
            spec = new PBEKeySpec(password, params.saltCopy(), params.iterations(), KEY_LENGTH_BYTES * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 failed", e);
        } finally {
            if (spec != null) {
                spec.clearPassword();
            }
        }
    }

    /** Known-vector helper for tests: derive and wipe internal copies. */
    public static byte[] deriveAndCopy(char[] masterPassword, KdfParameters params) {
        byte[] kek = deriveKek(masterPassword, params);
        byte[] copy = Arrays.copyOf(kek, kek.length);
        SecureMemory.wipe(kek);
        return copy;
    }
}
