package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayAnalytics {
    private LocalDate date;

    private List<ClassifiedRecord> records = new ArrayList<>();
    private List<SleepSession> sleepSessions = new ArrayList<>();

    private Duration workTime = Duration.ZERO;
    private Duration leisureTime = Duration.ZERO;
    private Duration restTime = Duration.ZERO;

    private Boolean isWorkDay = false;
    private Duration workDayDuration = Duration.ZERO;
    private Double focusScore = 0.0;

    private List<AnalyticsIssue> issues = new ArrayList<>();
}