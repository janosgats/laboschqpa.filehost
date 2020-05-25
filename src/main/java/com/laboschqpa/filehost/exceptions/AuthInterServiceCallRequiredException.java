package com.laboschqpa.filehost.exceptions;

public class AuthInterServiceCallRequiredException extends RuntimeException {
    public AuthInterServiceCallRequiredException() {
    }

    public AuthInterServiceCallRequiredException(String message) {
        super(message);
    }

    public AuthInterServiceCallRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthInterServiceCallRequiredException(Throwable cause) {
        super(cause);
    }

    public AuthInterServiceCallRequiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
