package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoBreaksService {

    private static final double NO_BREAKS_LUNCH_THRESHOLD = 8.0;
    private static final double NO_BREAKS_NO_LUNCH_THRESHOLD = 5.0;

    public Optional<AnalyticsIssue> detectNoBreaks(
            DayAnalytics day,
            List<TimePoint> points,
            List<WorkSession> sessions
    ) {

        if (!day.getIsWorkDay()) {
            return Optional.empty();
        }

        if (day.getWorkDayDuration().toMinutes() < 120) {
            return Optional.empty();
        }

        Duration maxBreakBetweenSessions = findLongestBreak(sessions);
        double threshold = maxBreakBetweenSessions.toMinutes() > 40 ? NO_BREAKS_NO_LUNCH_THRESHOLD : NO_BREAKS_LUNCH_THRESHOLD;

        Duration totalBreak = Duration.ZERO;
        for (WorkSession session : sessions) {
            totalBreak.plus(calculateBreaksInSession(session, points));
        }

        double breaksPerHour =
                breakMinutesPerWorkHour(totalBreak, day.getWorkDayDuration());

        if (breaksPerHour >= threshold) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.NO_BREAKS);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.BREAKS_MINUTES_PER_HOUR, breaksPerHour);
        params.put(IssueParam.TOTAL_BREAKS_MINUTES, totalBreak.toMinutes());

        issue.setParams(params);

        return Optional.of(issue);
    }

    private Duration findLongestBreak(List<WorkSession> sessions) {

        if (sessions == null || sessions.size() < 2) {
            return Duration.ZERO;
        }

        sessions.sort(Comparator.comparing(WorkSession::getStart));

        Duration longestBreak = Duration.ZERO;

        for (int i = 1; i < sessions.size(); i++) {

            WorkSession prev = sessions.get(i - 1);
            WorkSession curr = sessions.get(i);

            Duration gap = Duration.between(prev.getEnd(), curr.getStart());

            if (!gap.isNegative() && gap.compareTo(longestBreak) > 0) {
                longestBreak = gap;
            }
        }

        return longestBreak;
    }

    private Duration calculateBreaksInSession(
            WorkSession session,
            List<TimePoint> points
    ) {

        List<TimePoint> sessionPoints = points.stream()
                .filter(p ->
                        !p.getTime().isBefore(session.getStart()) &&
                                !p.getTime().isAfter(session.getEnd()) &&
                                p.getActivityClass() == ActivityClass.WORK)
                .sorted(Comparator.comparing(TimePoint::getTime))
                .collect(Collectors.toCollection(ArrayList::new));

        int activeWork = 0;

        ZonedDateTime breakStart = null;
        Duration totalBreak = Duration.ZERO;

        for (TimePoint p : sessionPoints) {

            if (p.isStart()) activeWork++;
            if (!p.isStart()) activeWork--;

            if (activeWork == 0 && breakStart == null) {
                breakStart = p.getTime();
            }

            if (activeWork > 0 && breakStart != null) {
                totalBreak = totalBreak.plus(
                        Duration.between(breakStart, p.getTime())
                );
                breakStart = null;
            }
        }

        if (breakStart != null) {
            totalBreak = totalBreak.plus(
                    Duration.between(breakStart, session.getEnd())
            );
        }

        return totalBreak;
    }

    private double breakMinutesPerWorkHour(Duration totalBreak, Duration workDayDuration) {

        if (workDayDuration.isZero()) {
            return 0;
        }

        double breakMinutes = totalBreak.toMinutes();
        double workHours = workDayDuration.toMinutes() / 60.0;

        return breakMinutes / workHours;
    }
}
