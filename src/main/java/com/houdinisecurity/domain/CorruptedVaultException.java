package com.houdinisecurity.domain;

public class CorruptedVaultException extends RuntimeException {

    public CorruptedVaultException(String message) {
        super(message);
    }

    public CorruptedVaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
