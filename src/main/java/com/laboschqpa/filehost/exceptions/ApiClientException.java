package com.laboschqpa.filehost.exceptions;

import org.springframework.http.HttpStatus;

public class ApiClientException extends RuntimeException {
    private HttpStatus httpStatus;

    public ApiClientException() {
    }

    public ApiClientException(Throwable e) {
        super(e);
    }

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ApiClientException(String message, Throwable e) {
        super(message, e);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
