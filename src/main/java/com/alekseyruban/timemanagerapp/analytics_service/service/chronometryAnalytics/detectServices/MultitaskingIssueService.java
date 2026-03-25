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
public class MultitaskingIssueService {

    private static final double MULTITASKING_THRESHOLD = 10.0;

    public Optional<AnalyticsIssue> detectMultitasking(
            DayAnalytics day,
            List<TimePoint> points,
            List<WorkSession> sessions
    ) {

        if (!day.getIsWorkDay()) {
            return Optional.empty();
        }

        Duration multitasking = calculateMultitaskOverlap(points, sessions);

        double multitaskingPerHour = calculateMultitaskPerHour(multitasking, day.getWorkTime());

        if (multitaskingPerHour < MULTITASKING_THRESHOLD) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.EXCESSIVE_MULTITASKING);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.MULTITASK_OVERLAP_MINUTES, multitasking.toMinutes());
        params.put(IssueParam.MULTITASK_OVERLAP_MINUTES_PER_HOUR, multitaskingPerHour);

        issue.setParams(params);

        return Optional.of(issue);
    }

    private Duration calculateMultitaskOverlap(
            List<TimePoint> points,
            List<WorkSession> sessions
    ) {
        Duration totalOverlap = Duration.ZERO;

        for (WorkSession session : sessions) {

            List<TimePoint> sessionPoints = points.stream()
                    .filter(p -> p.getActivityClass() == ActivityClass.WORK)
                    .filter(p -> !p.getTime().isBefore(session.getStart()) &&
                            !p.getTime().isAfter(session.getEnd()))
                    .sorted(Comparator.comparing(TimePoint::getTime))
                    .collect(Collectors.toCollection(ArrayList::new));

            int activeCount = 0;
            ZonedDateTime overlapStart = null;

            for (TimePoint point : sessionPoints) {

                if (point.isStart()) {
                    activeCount++;

                    if (activeCount == 2) {
                        overlapStart = point.getTime();
                    }

                } else {
                    if (activeCount == 2 && overlapStart != null) {
                        totalOverlap = totalOverlap.plus(
                                Duration.between(overlapStart, point.getTime())
                        );
                        overlapStart = null;
                    }

                    activeCount--;
                }
            }
        }

        return totalOverlap;
    }

    private double calculateMultitaskPerHour(Duration overlap, Duration workTime) {

        double hours = workTime.toMinutes() / 60.0;

        if (hours == 0) {
            return 0;
        }

        return overlap.toMinutes() / hours;
    }
}
