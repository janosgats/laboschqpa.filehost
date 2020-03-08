package com.laboschcst.filehost.exceptions;

public class ContentNotFoundApiException extends RuntimeException {
    public ContentNotFoundApiException() {
    }

    public ContentNotFoundApiException(String message) {
        super(message);
    }

    public ContentNotFoundApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentNotFoundApiException(Throwable cause) {
        super(cause);
    }

    public ContentNotFoundApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
