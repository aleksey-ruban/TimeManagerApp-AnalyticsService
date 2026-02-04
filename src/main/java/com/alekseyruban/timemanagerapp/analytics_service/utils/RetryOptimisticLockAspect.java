package com.alekseyruban.timemanagerapp.analytics_service.utils;

import com.alekseyruban.timemanagerapp.analytics_service.exception.ApiException;
import com.alekseyruban.timemanagerapp.analytics_service.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryOptimisticLockAspect {

    @Around("@annotation(retryAnnotation)")
    public Object around(ProceedingJoinPoint pjp, RetryOptimisticLock retryAnnotation) throws Throwable {
        int maxRetries = retryAnnotation.maxRetries();
        long delay = retryAnnotation.delayMillis();
        int attempts = 0;

        while (true) {
            try {
                return pjp.proceed();
            } catch (OptimisticLockingFailureException ex) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            ErrorCode.CONFLICT,
                            ex.getMessage()
                    );
                }
                Thread.sleep(delay);
            }
        }
    }
}