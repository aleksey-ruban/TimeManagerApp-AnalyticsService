package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InsufficientSleepService {

    private static final long MIN_SLEEP_DURATION_MINUTES = 420;

    public Optional<AnalyticsIssue> detectInsufficientSleep(
            DayAnalytics day
    ) {

        Duration totalSleep = day.getSleepSessions().stream()
                .map(SleepSession::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        if (totalSleep.toMinutes() >= MIN_SLEEP_DURATION_MINUTES) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.INSUFFICIENT_SLEEP);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.SLEEP_DEFICIT_MINUTES, MIN_SLEEP_DURATION_MINUTES - totalSleep.toMinutes());
        params.put(IssueParam.SLEEP_DURATION, totalSleep.toMinutes());

        issue.setParams(params);

        return Optional.of(issue);
    }

}
