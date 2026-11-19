package com.houdinisecurity.domain;

public class InvalidMasterPasswordException extends RuntimeException {

    public InvalidMasterPasswordException() {
        super("Invalid master password");
    }

    public InvalidMasterPasswordException(String message) {
        super(message);
    }

    public InvalidMasterPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
