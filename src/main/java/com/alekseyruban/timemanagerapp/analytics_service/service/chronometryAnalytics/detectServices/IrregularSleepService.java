package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IrregularSleepService {

    private static final Duration MAX_DEVIATION = Duration.ofMinutes(60);
    private static final int MIN_DAYS_WITH_DEVIATION = 3;

    public Optional<AnalyticsIssue> buildIrregularSleepIssue(Map<LocalDate, DayAnalytics> days) {
        List<LocalTime> bedTimes = days.values().stream()
                .map(day -> day.getSleepSessions().stream()
                        .max(Comparator.comparing(SleepSession::getDuration))
                        .map(SleepSession::getStart)
                        .map(ZonedDateTime::toLocalTime)
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        if (bedTimes.isEmpty()) return Optional.empty();

        double avgMinutes = calculateCircularMeanMinutes(bedTimes);

        long daysWithDeviation = bedTimes.stream()
                .filter(t -> circularDifferenceMinutes(t, avgMinutes) > MAX_DEVIATION.toMinutes())
                .count();

        if (daysWithDeviation < MIN_DAYS_WITH_DEVIATION) {
            return Optional.empty();
        }

        double avgStart = calculateCircularMeanMinutes(bedTimes);
        double startStdDev = calculateCircularStdDevMinutes(bedTimes);

        List<SleepSession> allSleepSessions = days.values().stream()
                .flatMap(day -> day.getSleepSessions().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        List<Double> sleepDurations = allSleepSessions.stream()
                .map(session -> session.getSleepDuration().toMinutes() * 1.0)
                .collect(Collectors.toCollection(ArrayList::new));
        double avgDuration = sleepDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double durationStdDev = Math.sqrt(sleepDurations.stream()
                .mapToDouble(v -> Math.pow(v - avgDuration, 2))
                .average()
                .orElse(0));

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.IRREGULAR_SLEEP);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setPeriodStart(days.keySet().iterator().next().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(days.keySet().stream().reduce((first, last) -> last).get().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.AVG_SLEEP_START_TIME, avgStart);
        params.put(IssueParam.SLEEP_START_STD_DEV, startStdDev);
        params.put(IssueParam.AVG_SLEEP_DURATION_TIME, avgDuration);
        params.put(IssueParam.SLEEP_DURATION_STD_DEV, durationStdDev);
        params.put(IssueParam.WEEKLY_OCCURRENCES, daysWithDeviation);
        params.put(IssueParam.MAX_CONSECUTIVE_DAYS, 0);

        issue.setParams(params);

        return Optional.of(issue);
    }

    private double calculateCircularMeanMinutes(List<LocalTime> times) {
        double avgSin = times.stream()
                .mapToDouble(t -> {
                    double angle = 2 * Math.PI * (t.toSecondOfDay() / 86400.0);
                    return Math.sin(angle);
                })
                .average()
                .orElse(0);

        double avgCos = times.stream()
                .mapToDouble(t -> {
                    double angle = 2 * Math.PI * (t.toSecondOfDay() / 86400.0);
                    return Math.cos(angle);
                })
                .average()
                .orElse(0);

        double meanAngle = Math.atan2(avgSin, avgCos);

        if (meanAngle < 0) {
            meanAngle += 2 * Math.PI;
        }

        return meanAngle * 1440 / (2 * Math.PI);
    }

    private double calculateCircularStdDevMinutes(List<LocalTime> times) {
        double avgSin = times.stream()
                .mapToDouble(t -> {
                    double angle = 2 * Math.PI * (t.toSecondOfDay() / 86400.0);
                    return Math.sin(angle);
                })
                .average()
                .orElse(0);

        double avgCos = times.stream()
                .mapToDouble(t -> {
                    double angle = 2 * Math.PI * (t.toSecondOfDay() / 86400.0);
                    return Math.cos(angle);
                })
                .average()
                .orElse(0);

        double r = Math.sqrt(avgSin * avgSin + avgCos * avgCos);

        double stdRadians = Math.sqrt(-2 * Math.log(r));

        return stdRadians * 1440 / (2 * Math.PI);
    }

    private double circularDifferenceMinutes(LocalTime time, double meanMinutes) {
        double value = time.getHour() * 60 + time.getMinute();

        double diff = Math.abs(value - meanMinutes);

        return Math.min(diff, 1440 - diff);
    }

}
