package com.laboschqpa.filehost.exceptions.fileserving;

public class FileIsNotAvailableException extends FileServingException {
    public FileIsNotAvailableException() {
    }

    public FileIsNotAvailableException(String message) {
        super(message);
    }

    public FileIsNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileIsNotAvailableException(Throwable cause) {
        super(cause);
    }

    public FileIsNotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
