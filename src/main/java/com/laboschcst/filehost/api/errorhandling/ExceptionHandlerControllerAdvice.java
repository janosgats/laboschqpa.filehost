package com.laboschcst.filehost.api.errorhandling;

import com.laboschcst.filehost.exceptions.ConflictingRequestDataApiException;
import com.laboschcst.filehost.exceptions.ContentNotFoundApiException;
import com.laboschcst.filehost.exceptions.UnAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger loggerOfChild = LoggerFactory.getLogger(ExceptionHandlerControllerAdvice.class);

    private final ApiErrorResponseBody contentNotFoundErrorResponseBody = new ApiErrorResponseBody("Content not found.");
    private final ApiErrorResponseBody conflictingRequestDataErrorResponseBody = new ApiErrorResponseBody("Conflicting request data.");
    private final ApiErrorResponseBody unAuthorizedErrorResponseBody = new ApiErrorResponseBody("You are not authorized for the requested operation.");
    private final ApiErrorResponseBody genericExceptionErrorResponseBody = new ApiErrorResponseBody("Error while executing API request.");

    @ExceptionHandler(ContentNotFoundApiException.class)
    protected ResponseEntity<ApiErrorResponseBody> handleContentNotFound(
            Exception e, WebRequest request) {
        loggerOfChild.error("ContentNotFoundApiException caught while executing api request!", e);
        return new ResponseEntity<>(contentNotFoundErrorResponseBody, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConflictingRequestDataApiException.class)
    protected ResponseEntity<ApiErrorResponseBody> handleConflictingRequestData(
            Exception e, WebRequest request) {
        loggerOfChild.error("ConflictingRequestDataApiException caught while executing api request!", e);
        return new ResponseEntity<>(conflictingRequestDataErrorResponseBody, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnAuthorizedException.class)
    protected ResponseEntity<ApiErrorResponseBody> handleUnAuthorized(
            Exception e, WebRequest request) {
        loggerOfChild.error("UnAuthorizedException caught while executing api request!", e);
        return new ResponseEntity<>(unAuthorizedErrorResponseBody, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiErrorResponseBody> handleGenericException(
            Exception e, WebRequest request) {
        loggerOfChild.error("Exception caught while executing api request!", e);
        return new ResponseEntity<>(genericExceptionErrorResponseBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
