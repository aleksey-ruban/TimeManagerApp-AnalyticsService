package com.alekseyruban.timemanagerapp.analytics_service.utils;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryOptimisticLock {
    int maxRetries() default 5;
    long delayMillis() default 50;
}