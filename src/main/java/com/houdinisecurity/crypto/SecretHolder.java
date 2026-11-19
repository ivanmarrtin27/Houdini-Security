package com.houdinisecurity.crypto;

import java.util.Arrays;
import java.util.Objects;

/**
 * AutoCloseable holder for sensitive char/byte material. Always wipe in {@link #close()}.
 */
public final class SecretHolder implements AutoCloseable {

    private char[] chars;
    private byte[] bytes;
    private boolean closed;

    private SecretHolder(char[] chars, byte[] bytes) {
        this.chars = chars;
        this.bytes = bytes;
    }

    public static SecretHolder ofChars(char[] source) {
        Objects.requireNonNull(source, "source");
        return new SecretHolder(Arrays.copyOf(source, source.length), null);
    }

    public static SecretHolder ofBytes(byte[] source) {
        Objects.requireNonNull(source, "source");
        return new SecretHolder(null, Arrays.copyOf(source, source.length));
    }

    public static SecretHolder wrapChars(char[] owned) {
        return new SecretHolder(Objects.requireNonNull(owned), null);
    }

    public static SecretHolder wrapBytes(byte[] owned) {
        return new SecretHolder(null, Objects.requireNonNull(owned));
    }

    public char[] chars() {
        ensureOpen();
        if (chars == null) {
            throw new IllegalStateException("No char secret");
        }
        return chars;
    }

    public byte[] bytes() {
        ensureOpen();
        if (bytes == null) {
            throw new IllegalStateException("No byte secret");
        }
        return bytes;
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("SecretHolder closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (chars != null) {
            Arrays.fill(chars, '\0');
            chars = null;
        }
        if (bytes != null) {
            Arrays.fill(bytes, (byte) 0);
            bytes = null;
        }
    }

    @Override
    public String toString() {
        return "SecretHolder[REDACTED]";
    }
}
