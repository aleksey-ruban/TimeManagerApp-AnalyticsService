package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.ClassifiedRecord;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.DayAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.SleepSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SleepSessionsService {

    public List<SleepSession> buildSleepSessions(
            List<ClassifiedRecord> records
    ) {

        List<ClassifiedRecord> normalRecords = records.stream()
                .filter(p -> p.getActivityClass() == ActivityClass.REST && p.getCategoryCode() != CategoryCode.SLEEP)
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .collect(Collectors.toCollection(ArrayList::new));

        List<ClassifiedRecord> priorityRecords = records.stream()
                .filter(p -> p.getCategoryCode() == CategoryCode.SLEEP)
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .collect(Collectors.toCollection(ArrayList::new));

        normalRecords = normalizeSleepGroup(normalRecords);
        priorityRecords = normalizeSleepGroup(priorityRecords);

        List<ClassifiedRecord> merged =
                mergeSleepByPriority(normalRecords, priorityRecords);


        merged = merged.stream()
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .collect(Collectors.toCollection(ArrayList::new));

        Duration gapThreshold = Duration.ofMinutes(60);
        Duration minSession = Duration.ofMinutes(30);

        List<SleepSession> sessions = new ArrayList<>();
        SleepSession current = null;

        for (ClassifiedRecord r : merged) {

            if (current == null) {
                current = new SleepSession(r.getStart(), r.getEnd());
                continue;
            }

            Duration gap = Duration.between(current.getEnd(), r.getStart());

            if (gap.compareTo(gapThreshold) <= 0) {
                current.extend(r.getEnd());
            } else {

                if (current.getDuration().compareTo(minSession) >= 0) {
                    sessions.add(current);
                }

                current = new SleepSession(r.getStart(), r.getEnd());
            }
        }

        if (current != null &&
                current.getDuration().compareTo(minSession) >= 0) {
            sessions.add(current);
        }

        calculateSleepSessionDurations(sessions, merged);

        return sessions;
    }

    public void assignSleepSessionsToDays(
            List<SleepSession> sleepSessions,
            Map<LocalDate, DayAnalytics> days
    ) {

        for (SleepSession session : sleepSessions) {

            Map<LocalDate, Duration> perDay = new HashMap<>();

            ZonedDateTime cursor = session.getStart();

            while (cursor.isBefore(session.getEnd())) {

                LocalDate date = cursor.toLocalDate();

                ZonedDateTime dayEnd = cursor.toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(cursor.getZone());

                ZonedDateTime segmentEnd =
                        dayEnd.isBefore(session.getEnd()) ? dayEnd : session.getEnd();

                Duration duration = Duration.between(cursor, segmentEnd);

                perDay.merge(date, duration, Duration::plus);

                cursor = segmentEnd;
            }

            LocalDate targetDay = perDay.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(session.getStart().toLocalDate());

            DayAnalytics day = days.get(targetDay);

            if (day != null) {
                day.getSleepSessions().add(session);
            }
        }
    }

    private List<ClassifiedRecord> normalizeSleepGroup(List<ClassifiedRecord> records) {

        List<ClassifiedRecord> sorted = records.stream()
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .map(this::copyRecord)
                .collect(Collectors.toCollection(ArrayList::new));

        List<ClassifiedRecord> result = new ArrayList<>();

        for (ClassifiedRecord current : sorted) {

            if (result.isEmpty()) {
                result.add(current);
                continue;
            }

            ClassifiedRecord last = result.getLast();

            if (!current.getStart().isAfter(last.getEnd())) {

                if (!current.getEnd().isAfter(last.getEnd())) {
                    continue;
                }

                current.setStart(last.getEnd());
            }

            result.add(current);
        }

        return result;
    }

    private List<ClassifiedRecord> mergeSleepByPriority(
            List<ClassifiedRecord> normalRecords,
            List<ClassifiedRecord> priorityRecords
    ) {

        List<ClassifiedRecord> result = new ArrayList<>(priorityRecords);

        int j = 0;

        for (ClassifiedRecord normal : normalRecords) {

            ClassifiedRecord current = copyRecord(normal);

            while (j < priorityRecords.size() &&
                    !priorityRecords.get(j).getEnd().isAfter(current.getStart())) {
                j++;
            }

            int k = j;

            List<ClassifiedRecord> fragments = new ArrayList<>();
            fragments.add(current);

            while (k < priorityRecords.size()) {

                ClassifiedRecord priority = priorityRecords.get(k);

                if (!priority.getStart().isBefore(current.getEnd())) {
                    break;
                }

                List<ClassifiedRecord> updated = new ArrayList<>();

                for (ClassifiedRecord fragment : fragments) {

                    ZonedDateTime overlapStart = max(fragment.getStart(), priority.getStart());
                    ZonedDateTime overlapEnd = min(fragment.getEnd(), priority.getEnd());

                    if (!overlapStart.isBefore(overlapEnd)) {
                        updated.add(fragment);
                        continue;
                    }

                    Duration overlap = Duration.between(overlapStart, overlapEnd);
                    Duration duration = Duration.between(fragment.getStart(), fragment.getEnd());

                    double ratio = (double) overlap.toMinutes() / duration.toMinutes();

                    if (ratio >= 0.7) {
                        continue;
                    }

                    if (fragment.getStart().isBefore(overlapStart)) {
                        updated.add(copy(fragment, fragment.getStart(), overlapStart));
                    }

                    if (fragment.getEnd().isAfter(overlapEnd)) {
                        updated.add(copy(fragment, overlapEnd, fragment.getEnd()));
                    }
                }

                fragments = updated;

                if (fragments.isEmpty()) {
                    break;
                }

                k++;
            }

            result.addAll(fragments);
        }

        return result.stream()
                .sorted(Comparator.comparing(ClassifiedRecord::getStart))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void calculateSleepSessionDurations(
            List<SleepSession> sessions,
            List<ClassifiedRecord> mergedRecords
    ) {
        for (SleepSession session : sessions) {
            Duration total = Duration.ZERO;

            for (ClassifiedRecord record : mergedRecords) {
                boolean isSleepRecord =
                        record.getActivityClass() == ActivityClass.REST ||
                                record.getCategoryCode() == CategoryCode.SLEEP;

                if (!isSleepRecord) continue;

                ZonedDateTime overlapStart = record.getStart().isAfter(session.getStart())
                        ? record.getStart()
                        : session.getStart();

                ZonedDateTime overlapEnd = record.getEnd().isBefore(session.getEnd())
                        ? record.getEnd()
                        : session.getEnd();

                if (!overlapStart.isAfter(overlapEnd)) {
                    total = total.plus(Duration.between(overlapStart, overlapEnd));
                }
            }

            session.setSleepDuration(total);
        }
    }

    private ClassifiedRecord copyRecord(ClassifiedRecord source) {

        ClassifiedRecord copy = new ClassifiedRecord(
                source.getId(),
                source.getStart(),
                source.getEnd(),
                source.getActivityClass(),
                source.getCategoryCode()
        );

        copy.setCategoryCode(source.getCategoryCode());

        return copy;
    }

    private ClassifiedRecord copy(
            ClassifiedRecord source,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        ClassifiedRecord copy = copyRecord(source);
        copy.setStart(start);
        copy.setEnd(end);
        return copy;
    }

    private ZonedDateTime max(ZonedDateTime a, ZonedDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private ZonedDateTime min(ZonedDateTime a, ZonedDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
