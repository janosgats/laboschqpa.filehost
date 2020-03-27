package com.laboschqpa.filehost.exceptions.fileserving;

public class FileServingException extends RuntimeException {
    public FileServingException() {
    }

    public FileServingException(String message) {
        super(message);
    }

    public FileServingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileServingException(Throwable cause) {
        super(cause);
    }

    public FileServingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
