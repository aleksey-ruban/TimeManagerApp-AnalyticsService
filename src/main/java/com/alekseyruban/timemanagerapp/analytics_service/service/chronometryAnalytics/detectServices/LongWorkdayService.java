package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LongWorkdayService {

    private static final int LONG_WORKDAY_DURATION_THRESHOLD = 9 * 60;

    public Optional<AnalyticsIssue> detectLongWorkday(
            DayAnalytics day
    ) {

        if (!day.getIsWorkDay() || day.getWorkDayDuration().toMinutes() < LONG_WORKDAY_DURATION_THRESHOLD) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.LONG_WORKDAY);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.OVERTIME_MINUTES, day.getWorkDayDuration().toMinutes() - LONG_WORKDAY_DURATION_THRESHOLD);

        issue.setParams(params);

        return Optional.of(issue);
    }

}
