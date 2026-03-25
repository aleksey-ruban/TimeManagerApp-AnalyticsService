package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class ClassifiedRecord {
    private Long id;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private ActivityClass activityClass;
    private CategoryCode categoryCode;

    public ClassifiedRecord(ZonedDateTime start, ZonedDateTime end) {
        this.start = start;
        this.end = end;
    }
}