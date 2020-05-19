package com.laboschqpa.filehost.config.advice;

import com.laboschqpa.filehost.exceptions.NotImplementedException;
import com.laboschqpa.filehost.exceptions.apiclient.ApiClientException;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExceptionWrapperAspect {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionWrapperAspect.class);

    @Around("@within(com.laboschqpa.filehost.config.annotation.ExceptionWrappedApiClient)")
    public Object apiClientExceptionWrapperAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (ApiClientException | NotImplementedException e) {
            throw e;//letting these exceptions pass through untouched
        } catch (Exception e) {
            logger.error("ExceptionWrapperAspect caught an exception in an ApiClient: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ApiClientException("Exception was thrown in an ApiClient: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

    @Around("@within(com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass)")
    public Object serviceableFileExceptionWrapperAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (FileServingException | NotImplementedException e) {
            throw e;//letting these exceptions pass through untouched
        } catch (Exception e) {
            logger.error("ExceptionWrapperAspect caught an exception in an ExceptionWrappedFileServingClass: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new FileServingException("Exception was thrown in an ExceptionWrappedFileServingClass: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }
}
