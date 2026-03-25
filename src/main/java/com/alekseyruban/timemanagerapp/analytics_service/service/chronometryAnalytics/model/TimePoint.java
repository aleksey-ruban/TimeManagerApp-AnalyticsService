package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class TimePoint {
    private Long recordId;
    private ZonedDateTime time;
    private boolean isStart;
    private ActivityClass activityClass;
}