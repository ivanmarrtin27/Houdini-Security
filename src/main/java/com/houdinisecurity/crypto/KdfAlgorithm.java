package com.houdinisecurity.crypto;

public enum KdfAlgorithm {
    ARGON2ID((byte) 1),
    PBKDF2_HMAC_SHA256((byte) 2);

    private final byte id;

    KdfAlgorithm(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }

    public static KdfAlgorithm fromId(byte id) {
        for (KdfAlgorithm a : values()) {
            if (a.id == id) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown KDF id: " + id);
    }
}
