package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyIssuesService {

    private static final List<IssueCode> AGGREGATED_WEEKLY_CODES = List.of(
            IssueCode.LOW_FOCUS,
            IssueCode.NO_BREAKS,
            IssueCode.TOO_MANY_TASK_SWITCHES,
            IssueCode.EXCESSIVE_MULTITASKING,
            IssueCode.FRAGMENTED_WORKDAY,
            IssueCode.LONG_WORKDAY,
            IssueCode.INSUFFICIENT_SLEEP
    );

    public List<AnalyticsIssue> buildWeeklyIssues(Map<LocalDate, DayAnalytics> days) {
        return AGGREGATED_WEEKLY_CODES.stream()
                .map(code -> buildWeeklyIssue(days, code))
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private long countOccurrences(
            Map<LocalDate, DayAnalytics> days,
            IssueCode code
    ) {
        return days.values().stream()
                .filter(day -> day.getIssues().stream()
                        .anyMatch(issue -> issue.getCode() == code))
                .count();
    }

    private int maxConsecutiveDays(
            Map<LocalDate, DayAnalytics> days,
            IssueCode code
    ) {
        int max = 0;
        int current = 0;

        for (DayAnalytics day : days.values()) {
            boolean present = day.getIssues().stream()
                    .anyMatch(issue -> issue.getCode() == code);

            if (present) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }

        return max;
    }

    private boolean isSystemic(
            Map<LocalDate, DayAnalytics> days,
            IssueCode code
    ) {
        long count = countOccurrences(days, code);
        int consecutive = maxConsecutiveDays(days, code);

        return count >= 3 || consecutive >= 2;
    }

    private Optional<AnalyticsIssue> buildWeeklyIssue(
            Map<LocalDate, DayAnalytics> days,
            IssueCode code
    ) {
        if (!isSystemic(days, code)) {
            return Optional.empty();
        }

        List<AnalyticsIssue> matchingIssues = days.values().stream()
                .flatMap(day -> day.getIssues().stream())
                .filter(issue -> issue.getCode() == code)
                .collect(Collectors.toCollection(ArrayList::new));

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(code);
        issue.setSeverity(IssueSeverity.MEDIUM);

        LocalDate start = days.keySet().iterator().next();
        LocalDate end = start.plusDays(days.size());
        issue.setPeriodStart(start.atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(end.atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>(aggregateParams(matchingIssues, code));
        params.put(IssueParam.WEEKLY_OCCURRENCES, countOccurrences(days, code));
        params.put(IssueParam.MAX_CONSECUTIVE_DAYS, maxConsecutiveDays(days, code));
        issue.setParams(params);

        return Optional.of(issue);
    }

    private Map<IssueParam, Object> aggregateParams(
            List<AnalyticsIssue> issues,
            IssueCode code
    ) {
        return switch (code) {
            case LOW_FOCUS -> aggregateLowFocus(issues);
            case NO_BREAKS -> aggregateNoBreaks(issues);
            case TOO_MANY_TASK_SWITCHES -> aggregateTaskSwitches(issues);
            case EXCESSIVE_MULTITASKING -> aggregateMultitasking(issues);
            case FRAGMENTED_WORKDAY -> aggregateFragmentedWorkday(issues);
            case LONG_WORKDAY -> aggregateLongWorkday(issues);
            case INSUFFICIENT_SLEEP -> aggregateSleep(issues);
            case NO_DAYS_OFF, IRREGULAR_SLEEP -> Map.of();
        };
    }

    private Map<IssueParam, Object> aggregateLowFocus(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.FOCUS_SCORE, avg(issues, IssueParam.FOCUS_SCORE));
        params.put(IssueParam.FOCUS_LOSS_MINUTES, sum(issues, IssueParam.FOCUS_LOSS_MINUTES));

        return params;
    }

    private Map<IssueParam, Object> aggregateNoBreaks(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.TOTAL_BREAKS_MINUTES, sum(issues, IssueParam.TOTAL_BREAKS_MINUTES));
        params.put(IssueParam.BREAKS_MINUTES_PER_HOUR, avg(issues, IssueParam.BREAKS_MINUTES_PER_HOUR));

        return params;
    }

    private Map<IssueParam, Object> aggregateTaskSwitches(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.TASK_SWITCHES_COUNT, sum(issues, IssueParam.TASK_SWITCHES_COUNT));
        params.put(IssueParam.TASK_SWITCHES_PER_HOUR, avg(issues, IssueParam.TASK_SWITCHES_PER_HOUR));
        params.put(IssueParam.AVG_TIME_PER_TASK, avg(issues, IssueParam.AVG_TIME_PER_TASK));

        return params;
    }

    private Map<IssueParam, Object> aggregateMultitasking(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.MULTITASK_OVERLAP_MINUTES, sum(issues, IssueParam.MULTITASK_OVERLAP_MINUTES));
        params.put(IssueParam.MULTITASK_OVERLAP_MINUTES_PER_HOUR,
                avg(issues, IssueParam.MULTITASK_OVERLAP_MINUTES_PER_HOUR));

        return params;
    }

    private Map<IssueParam, Object> aggregateFragmentedWorkday(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.WORK_SESSION_COUNT, sum(issues, IssueParam.WORK_SESSION_COUNT));
        params.put(IssueParam.AVR_SESSION_DURATION, avg(issues, IssueParam.AVR_SESSION_DURATION));
        params.put(IssueParam.AVG_SESSION_GAP, avg(issues, IssueParam.AVG_SESSION_GAP));

        return params;
    }

    private Map<IssueParam, Object> aggregateLongWorkday(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.OVERTIME_MINUTES, sum(issues, IssueParam.OVERTIME_MINUTES));

        return params;
    }

    private Map<IssueParam, Object> aggregateSleep(List<AnalyticsIssue> issues) {
        Map<IssueParam, Object> params = new HashMap<>();

        params.put(IssueParam.SLEEP_DEFICIT_MINUTES, sum(issues, IssueParam.SLEEP_DEFICIT_MINUTES));
        params.put(IssueParam.AVG_SLEEP_DURATION, avg(issues, IssueParam.SLEEP_DURATION));

        return params;
    }

    private double avg(List<AnalyticsIssue> issues, IssueParam param) {
        return issues.stream()
                .map(i -> (Number) i.getParams().get(param))
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0);
    }

    private long sum(List<AnalyticsIssue> issues, IssueParam param) {
        return issues.stream()
                .map(i -> (Number) i.getParams().get(param))
                .filter(Objects::nonNull)
                .mapToLong(Number::longValue)
                .sum();
    }

}
