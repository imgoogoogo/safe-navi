package com.safenavi.backend.sync;

public class PublicDataException extends RuntimeException {

    public PublicDataException(String message) {
        super(message);
    }

    public PublicDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
