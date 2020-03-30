package com.laboschqpa.filehost.exceptions.fileserving;

public class StreamLengthLimitExceededException extends FileServingException {
    public StreamLengthLimitExceededException() {
    }

    public StreamLengthLimitExceededException(String message) {
        super(message);
    }

    public StreamLengthLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamLengthLimitExceededException(Throwable cause) {
        super(cause);
    }

    public StreamLengthLimitExceededException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
