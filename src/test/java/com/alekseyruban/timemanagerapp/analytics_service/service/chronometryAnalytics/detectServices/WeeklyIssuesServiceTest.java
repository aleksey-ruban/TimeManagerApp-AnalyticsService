package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.DayAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueParam;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueSeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyIssuesServiceTest {

    private final WeeklyIssuesService weeklyIssuesService = new WeeklyIssuesService();

    @Test
    void buildWeeklyIssuesAggregatesDurationParamsAsMinutes() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), dayWithFragmentedWorkday(3, Duration.ofMinutes(90), Duration.ofMinutes(30)));
        days.put(LocalDate.of(2026, 4, 2), dayWithFragmentedWorkday(4, Duration.ofMinutes(60), Duration.ofMinutes(20)));
        days.put(LocalDate.of(2026, 4, 3), dayWithFragmentedWorkday(5, Duration.ofMinutes(30), Duration.ofMinutes(10)));

        List<AnalyticsIssue> issues = weeklyIssuesService.buildWeeklyIssues(days);

        AnalyticsIssue weeklyIssue = issues.stream()
                .filter(issue -> issue.getCode() == IssueCode.FRAGMENTED_WORKDAY)
                .findFirst()
                .orElseThrow();

        assertEquals(12L, weeklyIssue.getParams().get(IssueParam.WORK_SESSION_COUNT));
        assertEquals(60.0, weeklyIssue.getParams().get(IssueParam.AVR_SESSION_DURATION));
        assertEquals(20.0, weeklyIssue.getParams().get(IssueParam.AVG_SESSION_GAP));
        assertEquals(3L, weeklyIssue.getParams().get(IssueParam.WEEKLY_OCCURRENCES));
    }

    private DayAnalytics dayWithFragmentedWorkday(int sessionCount, Duration avgSessionDuration, Duration avgSessionGap) {
        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.FRAGMENTED_WORKDAY);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setPeriodStart(Instant.parse("2026-04-01T00:00:00Z"));
        issue.setPeriodEnd(Instant.parse("2026-04-02T00:00:00Z"));
        issue.setParams(Map.of(
                IssueParam.WORK_SESSION_COUNT, sessionCount,
                IssueParam.AVR_SESSION_DURATION, avgSessionDuration,
                IssueParam.AVG_SESSION_GAP, avgSessionGap
        ));

        DayAnalytics dayAnalytics = new DayAnalytics();
        dayAnalytics.setIssues(List.of(issue));
        return dayAnalytics;
    }
}
