package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManyTaskSwitchesService {

    private static final double TOO_MANY_TASK_SWITCHES_THRESHOLD = 6.0;

    public Optional<AnalyticsIssue> detectManyTaskSwitches(
            DayAnalytics day,
            List<WorkSession> sessions
    ) {

        if (!day.getIsWorkDay()) {
            return Optional.empty();
        }

        Long workSwitches = calculateWorkSwitches(day.getRecords(), sessions);

        double switchedPerHour = calculateSwitchesPerHour(workSwitches, day.getWorkTime());

        if (switchedPerHour < TOO_MANY_TASK_SWITCHES_THRESHOLD) {
            return Optional.empty();
        }

        AnalyticsIssue issue = new AnalyticsIssue();
        issue.setCode(IssueCode.TOO_MANY_TASK_SWITCHES);
        issue.setSeverity(IssueSeverity.MEDIUM);

        issue.setPeriodStart(day.getDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        issue.setPeriodEnd(day.getDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Map<IssueParam, Object> params = new HashMap<>();
        params.put(IssueParam.TASK_SWITCHES_COUNT, workSwitches);
        params.put(IssueParam.TASK_SWITCHES_PER_HOUR, switchedPerHour);
        params.put(IssueParam.AVG_TIME_PER_TASK, calculateAverageTaskDuration(day.getRecords()));

        issue.setParams(params);

        return Optional.of(issue);
    }

    private Long calculateWorkSwitches(
            List<ClassifiedRecord> records,
            List<WorkSession> sessions
    ) {
        Long switches = 0L;

        for (WorkSession session : sessions) {

            List<ClassifiedRecord> sessionRecords = records.stream()
                    .filter(r -> r.getActivityClass() == ActivityClass.WORK)
                    .filter(r -> !r.getStart().isAfter(session.getEnd()) &&
                            !r.getEnd().isBefore(session.getStart()))
                    .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                    .collect(Collectors.toCollection(ArrayList::new));

            ClassifiedRecord previous = null;

            for (ClassifiedRecord current : sessionRecords) {

                if (previous != null) {

                    Duration gap = Duration.between(previous.getEnd(), current.getStart());

                    boolean overlaps = !current.getStart().isAfter(previous.getEnd());

                    boolean smallGap = gap.toMinutes() <= 5;

                    boolean differentTask = !previous.getId().equals(current.getId());

                    if (differentTask && (overlaps || smallGap)) {
                        switches++;
                    }
                }

                previous = current;
            }
        }

        return switches;
    }

    private double calculateSwitchesPerHour(long switches, Duration workTime) {
        double hours = workTime.toMinutes() / 60.0;

        if (hours == 0) {
            return 0;
        }

        return switches / hours;
    }

    private Duration calculateAverageTaskDuration(List<ClassifiedRecord> records) {

        List<ClassifiedRecord> workRecords = records.stream()
                .filter(r -> r.getActivityClass() == ActivityClass.WORK)
                .collect(Collectors.toCollection(ArrayList::new));

        if (workRecords.isEmpty()) {
            return Duration.ZERO;
        }

        Duration total = workRecords.stream()
                .map(r -> Duration.between(r.getStart(), r.getEnd()))
                .reduce(Duration.ZERO, Duration::plus);

        return total.dividedBy(workRecords.size());
    }
}
