package com.laboschqpa.filehost.exceptions;

public class FileServingRateLimitHitException extends RuntimeException {
    public FileServingRateLimitHitException(String message) {
        super(message);
    }
    public FileServingRateLimitHitException() {
        super();
    }
}
