package com.laboschqpa.filehost.exceptions;

public class AuthInterServiceException extends RuntimeException {
    public AuthInterServiceException() {
    }

    public AuthInterServiceException(String message) {
        super(message);
    }

    public AuthInterServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthInterServiceException(Throwable cause) {
        super(cause);
    }

    public AuthInterServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
