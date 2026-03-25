package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsIssue {
    private IssueCode code;
    private IssueSeverity severity;
    private Map<IssueParam, Object> params = new HashMap<>();
    private Instant periodStart;
    private Instant periodEnd;
    private String recommendation;
}
