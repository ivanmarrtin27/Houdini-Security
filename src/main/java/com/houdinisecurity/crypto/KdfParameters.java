package com.houdinisecurity.crypto;

public record KdfParameters(
        KdfAlgorithm algorithm,
        int memoryKiB,
        int iterations,
        int parallelism,
        byte[] salt
) {
    public static final int DEFAULT_ARGON2_MEMORY_KIB = 65536; // 64 MiB
    public static final int DEFAULT_ARGON2_ITERATIONS = 3;
    public static final int DEFAULT_ARGON2_PARALLELISM = 2;
    public static final int DEFAULT_PBKDF2_ITERATIONS = 600_000;
    public static final int MIN_SALT_LENGTH = 16;

    public KdfParameters {
        if (salt == null || salt.length < MIN_SALT_LENGTH) {
            throw new IllegalArgumentException("salt must be >= " + MIN_SALT_LENGTH);
        }
        if (algorithm == KdfAlgorithm.ARGON2ID) {
            if (memoryKiB < DEFAULT_ARGON2_MEMORY_KIB) {
                throw new IllegalArgumentException("Argon2 memoryKiB must be >= " + DEFAULT_ARGON2_MEMORY_KIB);
            }
            if (iterations < DEFAULT_ARGON2_ITERATIONS) {
                throw new IllegalArgumentException("Argon2 iterations must be >= " + DEFAULT_ARGON2_ITERATIONS);
            }
            if (parallelism < 2 || parallelism > 4) {
                throw new IllegalArgumentException("Argon2 parallelism must be 2..4");
            }
        } else if (algorithm == KdfAlgorithm.PBKDF2_HMAC_SHA256) {
            if (iterations < DEFAULT_PBKDF2_ITERATIONS) {
                throw new IllegalArgumentException("PBKDF2 iterations must be >= " + DEFAULT_PBKDF2_ITERATIONS);
            }
        }
        salt = salt.clone();
    }

    public static KdfParameters argon2Defaults(byte[] salt) {
        return new KdfParameters(
                KdfAlgorithm.ARGON2ID,
                DEFAULT_ARGON2_MEMORY_KIB,
                DEFAULT_ARGON2_ITERATIONS,
                DEFAULT_ARGON2_PARALLELISM,
                salt
        );
    }

    public static KdfParameters pbkdf2Defaults(byte[] salt) {
        return new KdfParameters(
                KdfAlgorithm.PBKDF2_HMAC_SHA256,
                0,
                DEFAULT_PBKDF2_ITERATIONS,
                0,
                salt
        );
    }

    public byte[] saltCopy() {
        return salt.clone();
    }
}
