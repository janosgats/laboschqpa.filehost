package com.laboschqpa.filehost.exceptions.fileserving;

public class InvalidStoredFileException extends FileServingException {
    public InvalidStoredFileException() {
    }

    public InvalidStoredFileException(String message) {
        super(message);
    }

    public InvalidStoredFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidStoredFileException(Throwable cause) {
        super(cause);
    }

    public InvalidStoredFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
