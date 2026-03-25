package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FragmentedWorkdayService {

    public Optional<AnalyticsIssue> detectFragmentedWorkday(
            DayAnalytics day,
            List<WorkSession> sessions
    ) {

        if (!day.getIsWorkDay() || sessions.size() < 3) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.FRAGMENTED_WORKDAY);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.WORK_SESSION_COUNT, sessions.size());
        params.put(IssueParam.AVR_SESSION_DURATION, calculateAverageSessionDuration(sessions));
        params.put(IssueParam.AVG_SESSION_GAP, calculateAverageSessionGap(sessions));

        issue.setParams(params);

        return Optional.of(issue);
    }

    private Duration calculateAverageSessionDuration(List<WorkSession> sessions) {

        if (sessions.isEmpty()) {
            return Duration.ZERO;
        }

        Duration total = sessions.stream()
                .map(WorkSession::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        return total.dividedBy(sessions.size());
    }

    private Duration calculateAverageSessionGap(List<WorkSession> sessions) {

        if (sessions.size() < 2) {
            return Duration.ZERO;
        }

        Duration totalGap = Duration.ZERO;

        for (int i = 1; i < sessions.size(); i++) {

            WorkSession previous = sessions.get(i - 1);
            WorkSession current = sessions.get(i);

            Duration gap = Duration.between(previous.getEnd(), current.getStart());

            totalGap = totalGap.plus(gap);
        }

        return totalGap.dividedBy(sessions.size() - 1);
    }
}
