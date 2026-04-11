package com.alekseyruban.timemanagerapp.analytics_service.entity;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueParam;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueSeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsIssueEntityTest {

    @Test
    void fromDomainNormalizesDurationParamsToMinutes() {
        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.FRAGMENTED_WORKDAY);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setParams(Map.of(
                IssueParam.WORK_SESSION_COUNT, 4,
                IssueParam.AVR_SESSION_DURATION, Duration.ofMinutes(90)
        ));

        AnalyticsIssueEntity entity = AnalyticsIssueEntity.fromDomain(issue, null, null);

        assertEquals(4, entity.getParams().get(IssueParam.WORK_SESSION_COUNT.name()));
        assertEquals(90L, entity.getParams().get(IssueParam.AVR_SESSION_DURATION.name()));
    }
}
