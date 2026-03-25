package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.ClassifiedRecord;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.WorkSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkSessionsService {

    public List<WorkSession> buildWorkSessions(List<ClassifiedRecord> records) {
        Duration gapThreshold = Duration.ofMinutes(60);
        Duration minSession = Duration.ofMinutes(15);

        List<ClassifiedRecord> workRecords = mergeIntervals(records);

        List<WorkSession> sessions = new ArrayList<>();
        WorkSession current = null;

        for (ClassifiedRecord r : workRecords) {

            if (current == null) {
                current = new WorkSession(r.getStart(), r.getEnd());
                continue;
            }

            Duration gap = Duration.between(current.getEnd(), r.getStart());

            if (gap.compareTo(gapThreshold) <= 0) {
                current.extend(r.getEnd());
            } else {
                if (current.getDuration().compareTo(minSession) >= 0) {
                    sessions.add(current);
                }
                current = new WorkSession(r.getStart(), r.getEnd());
            }
        }

        if (current != null && current.getDuration().compareTo(minSession) >= 0) {
            sessions.add(current);
        }

        return sessions;
    }

    private List<ClassifiedRecord> mergeIntervals(List<ClassifiedRecord> records) {

        List<ClassifiedRecord> intervals = records.stream()
                .filter(r -> r.getActivityClass() == ActivityClass.WORK)
                .map(r -> new ClassifiedRecord(r.getStart(), r.getEnd()))
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .collect(Collectors.toCollection(ArrayList::new));

        List<ClassifiedRecord> merged = new ArrayList<>();

        for (ClassifiedRecord interval : intervals) {

            if (merged.isEmpty()) {
                merged.add(interval);
                continue;
            }

            ClassifiedRecord last = merged.getLast();

            if (!interval.getStart().isAfter(last.getEnd())) {

                ZonedDateTime newEnd = interval.getEnd().isAfter(last.getEnd())
                        ? interval.getEnd()
                        : last.getEnd();

                merged.set(merged.size() - 1,
                        new ClassifiedRecord(last.getStart(), newEnd));

            } else {
                merged.add(interval);
            }
        }

        return merged;
    }

}
