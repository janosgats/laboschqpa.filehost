package com.laboschqpa.filehost.exceptions.fileserving;

public class FileSavingException extends FileServingException {
    public FileSavingException() {
    }

    public FileSavingException(String message) {
        super(message);
    }

    public FileSavingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSavingException(Throwable cause) {
        super(cause);
    }

    public FileSavingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
