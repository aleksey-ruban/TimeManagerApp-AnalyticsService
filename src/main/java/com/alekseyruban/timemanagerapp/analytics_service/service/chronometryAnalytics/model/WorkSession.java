package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import lombok.Data;

import java.time.Duration;
import java.time.ZonedDateTime;

@Data
public class WorkSession {

    private ZonedDateTime start;
    private ZonedDateTime end;

    public WorkSession(ZonedDateTime start, ZonedDateTime end) {
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