package com.laboschqpa.filehost.exceptions;

public class ConflictingRequestDataApiException extends RuntimeException {
    public ConflictingRequestDataApiException() {
    }

    public ConflictingRequestDataApiException(String message) {
        super(message);
    }

    public ConflictingRequestDataApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictingRequestDataApiException(Throwable cause) {
        super(cause);
    }

    public ConflictingRequestDataApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
