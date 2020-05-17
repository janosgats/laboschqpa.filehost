package com.laboschqpa.filehost.exceptions.apiclient;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResponseCodeIsNotSuccessApiClientException extends ApiClientException {
    private HttpStatus httpStatus;
    private String responseBody;

    @Builder
    public ResponseCodeIsNotSuccessApiClientException(String message, HttpStatus httpStatus, String responseBody, Throwable e) {
        super(message, e);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }
}
