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
public class LowFocusService {

    private static final double LOW_FOCUS_THRESHOLD = 0.55;

    public Optional<AnalyticsIssue> detectLowFocus(DayAnalytics day) {

        if (!day.getIsWorkDay()) {
            return Optional.empty();
        }

        double focusScore = day.getFocusScore();

        if (focusScore >= LOW_FOCUS_THRESHOLD) {
            return Optional.empty();
        }

        Duration workTime = day.getWorkTime();
        Duration workDay = day.getWorkDayDuration();

        Integer focusLoss = (int) (workDay.toMinutes() * 0.65 - workTime.toMinutes());

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.LOW_FOCUS);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.FOCUS_SCORE, focusScore);
        params.put(IssueParam.FOCUS_LOSS_MINUTES, focusLoss);

        issue.setParams(params);

        return Optional.of(issue);
    }

}
