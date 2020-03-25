package com.laboschqpa.filehost.exceptions;

public class InvalidHttpRequestException extends RuntimeException {
    public InvalidHttpRequestException() {
    }

    public InvalidHttpRequestException(String message) {
        super(message);
    }

    public InvalidHttpRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidHttpRequestException(Throwable cause) {
        super(cause);
    }

    public InvalidHttpRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
