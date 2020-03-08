package com.laboschcst.filehost.config.advice;

import com.laboschcst.filehost.exceptions.ApiClientException;
import com.laboschcst.filehost.exceptions.NotImplementedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiClientExceptionAspect {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientExceptionAspect.class);

    @Around("@within(com.laboschcst.filehost.config.annotation.HandledApiClient)")
    public Object rdbAccessAroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (ApiClientException | NotImplementedException e) {
            throw e;//letting these exceptions pass through untouched
        } catch (Exception e) {
            logger.error("ApiClientExceptionAspect caught an exception: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ApiClientException("Exception was thrown in an ApiClient!", e);
        }
    }
}
