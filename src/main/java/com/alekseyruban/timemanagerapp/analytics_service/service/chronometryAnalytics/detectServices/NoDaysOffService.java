package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoDaysOffService {

    public Optional<AnalyticsIssue> buildNoDaysOffIssue(Map<LocalDate, DayAnalytics> days) {
        long daysOff = days.values().stream()
                .filter(day -> !Boolean.TRUE.equals(day.getIsWorkDay()))
                .count();

        if (daysOff >= 2) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.NO_DAYS_OFF);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setPeriodStart(days.keySet().iterator().next().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(days.keySet().stream().reduce((first, last) -> last).get().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.TOTAL_WORK_DAYS, days.size() - daysOff);
        params.put(IssueParam.TOTAL_DAYS_OFF_COUNT, daysOff);
        params.put(IssueParam.WEEKLY_OCCURRENCES, daysOff);
        params.put(IssueParam.MAX_CONSECUTIVE_DAYS, 0);

        issue.setParams(params);

        return Optional.of(issue);
    }

}
