package com.laboschqpa.filehost.api.errorhandling;

import com.laboschqpa.filehost.exceptions.ConflictingRequestDataApiException;
import com.laboschqpa.filehost.exceptions.FileServingRateLimitHitException;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ApiErrorDescriptorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger loggerOfChild = LoggerFactory.getLogger(ExceptionHandlerControllerAdvice.class);

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        loggerOfChild.error("Cannot parse incoming HTTP message!", ex);

        return new ResponseEntity<>(new ApiErrorResponseBody(ex.getMessage()), headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ApiErrorDescriptorException.class})
    protected ResponseEntity<ApiErrorResponseBody> handleApiErrorDescriptorException(
            ApiErrorDescriptorException e, WebRequest request) {
        loggerOfChild.trace("handleApiErrorDescriptorException() caught exception while executing api request!", e);
        return new ResponseEntity<>(new ApiErrorResponseBody(e.getApiErrorDescriptor(), e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FileServingRateLimitHitException.class)
    protected ResponseEntity<String> handleFileServingRateLimitHit(
            Exception e, WebRequest request) {
        return new ResponseEntity<>("File serving rate limit hit", HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(ConflictingRequestDataApiException.class)
    protected ResponseEntity<ApiErrorResponseBody> handleConflictingRequestData(
            Exception e, WebRequest request) {
        loggerOfChild.error("ConflictingRequestDataApiException caught while executing api request!", e);
        return new ResponseEntity<>(new ApiErrorResponseBody(e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnAuthorizedException.class)
    protected ResponseEntity<ApiErrorResponseBody> handleUnAuthorized(
            Exception e, WebRequest request) {
        loggerOfChild.trace("UnAuthorizedException caught while executing api request!", e);
        return new ResponseEntity<>(new ApiErrorResponseBody(e.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiErrorResponseBody> handleGenericException(
            Exception e, WebRequest request) {
        loggerOfChild.error("Exception caught while executing api request!", e);
        return new ResponseEntity<>(new ApiErrorResponseBody(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
