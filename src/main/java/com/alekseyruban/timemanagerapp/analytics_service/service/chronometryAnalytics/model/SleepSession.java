package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import lombok.Data;

import java.time.Duration;
import java.time.ZonedDateTime;

@Data
public class SleepSession {

    private ZonedDateTime start;
    private ZonedDateTime end;
    private Duration sleepDuration;

    public SleepSession(ZonedDateTime start, ZonedDateTime end) {
        this.start = start;
        this.end = end;
    }

    public void extend(ZonedDateTime newEnd) {
        if (newEnd.isAfter(end)) {
            end = newEnd;
        }
    }

    public Duration getDuration() {
        return Duration.between(start, end);
    }
}