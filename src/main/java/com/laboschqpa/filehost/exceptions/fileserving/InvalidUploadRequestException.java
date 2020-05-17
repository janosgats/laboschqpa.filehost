package com.laboschqpa.filehost.exceptions.fileserving;

public class InvalidUploadRequestException extends FileServingException {
    public InvalidUploadRequestException() {
    }

    public InvalidUploadRequestException(String message) {
        super(message);
    }

    public InvalidUploadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidUploadRequestException(Throwable cause) {
        super(cause);
    }

    public InvalidUploadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
