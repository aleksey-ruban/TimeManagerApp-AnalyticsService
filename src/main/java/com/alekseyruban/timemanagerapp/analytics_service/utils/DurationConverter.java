package com.alekseyruban.timemanagerapp.analytics_service.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Duration;

@Converter(autoApply = false)
public class DurationConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration != null ? duration.getSeconds() : null;
    }

    @Override
    public Duration convertToEntityAttribute(Long value) {
        return value != null ? Duration.ofSeconds(value) : Duration.ZERO;
    }
}