package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.ClassifiedRecord;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.DayAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.SleepSession;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.WorkSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionBuildersTest {

    private final WorkSessionsService workSessionsService = new WorkSessionsService();
    private final SleepSessionsService sleepSessionsService = new SleepSessionsService();
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    @Test
    void buildWorkSessionsReturnsEmptyWhenNoWorkRecords() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 8, 9, ActivityClass.REST, CategoryCode.SLEEP)
        ));

        assertThat(sessions).isEmpty();
    }

    @Test
    void buildWorkSessionsMergesOverlappingIntervals() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 9, 10, ActivityClass.WORK, CategoryCode.WORK),
                record(2, 9, 30, 11, 0, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).singleElement()
                .satisfies(session -> assertThat(session.getDuration()).isEqualTo(Duration.ofHours(2)));
    }

    @Test
    void buildWorkSessionsMergesIntervalsSeparatedBySmallGap() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 9, 10, ActivityClass.WORK, CategoryCode.WORK),
                record(2, 10, 45, 12, 0, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).hasSize(1);
        assertThat(sessions.getFirst().getDuration()).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void buildWorkSessionsSplitsIntervalsSeparatedByLongGap() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 9, 10, ActivityClass.WORK, CategoryCode.WORK),
                record(2, 11, 30, 12, 30, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).hasSize(2);
    }

    @Test
    void buildWorkSessionsFiltersOutShortSessions() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 9, 10, ActivityClass.WORK, CategoryCode.WORK),
                record(2, 13, 0, 13, 10, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).hasSize(1);
    }

    @Test
    void buildWorkSessionsSortsInputBeforeProcessing() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(2, 14, 15, ActivityClass.WORK, CategoryCode.WORK),
                record(1, 9, 10, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).extracting(WorkSession::getStart).isSorted();
    }

    @Test
    void buildWorkSessionsMergesWhenGapIsExactlyThreshold() {
        List<WorkSession> sessions = workSessionsService.buildWorkSessions(List.of(
                record(1, 9, 0, 10, 0, ActivityClass.WORK, CategoryCode.WORK),
                record(2, 11, 0, 12, 0, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).hasSize(1);
    }

    @Test
    void workSessionExtendDoesNotShrinkEnd() {
        WorkSession session = new WorkSession(time(9, 0), time(10, 0));

        session.extend(time(9, 30));

        assertThat(session.getEnd()).isEqualTo(time(10, 0));
    }

    @Test
    void buildSleepSessionsUsesRestAndExplicitSleepRecords() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 23, 0, 2, 0, ActivityClass.REST, CategoryCode.SLEEP),
                record(2, 2, 10, 6, 30, ActivityClass.REST, CategoryCode.LEISURE)
        ));

        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(SleepSession::getDuration)
                .containsExactly(Duration.ofHours(4).plusMinutes(20), Duration.ofHours(3));
    }

    @Test
    void buildSleepSessionsIgnoresNonRestNonSleepRecords() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 10, 11, ActivityClass.WORK, CategoryCode.WORK)
        ));

        assertThat(sessions).isEmpty();
    }

    @Test
    void buildSleepSessionsDropsShortSleepFragments() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 1, 0, 1, 20, ActivityClass.REST, CategoryCode.LEISURE)
        ));

        assertThat(sessions).isEmpty();
    }

    @Test
    void buildSleepSessionsSplitsAcrossLargeGap() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 0, 0, 2, 0, ActivityClass.REST, CategoryCode.SLEEP),
                record(2, 4, 30, 7, 0, ActivityClass.REST, CategoryCode.SLEEP)
        ));

        assertThat(sessions).hasSize(2);
    }

    @Test
    void buildSleepSessionsKeepsPrioritySleepIntervalsOverMostlyOverlappingRestIntervals() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 22, 0, 6, 0, ActivityClass.REST, CategoryCode.SLEEP),
                record(2, 22, 30, 5, 30, ActivityClass.REST, CategoryCode.LEISURE)
        ));

        assertThat(sessions).singleElement()
                .satisfies(session -> assertThat(session.getDuration()).isEqualTo(Duration.ofHours(8)));
    }

    @Test
    void buildSleepSessionsKeepsNonOverlappingFragmentsWhenPriorityCoversLessThanSeventyPercent() {
        List<SleepSession> sessions = sleepSessionsService.buildSleepSessions(List.of(
                record(1, 22, 0, 6, 0, ActivityClass.REST, CategoryCode.LEISURE),
                record(2, 23, 0, 2, 0, ActivityClass.REST, CategoryCode.SLEEP)
        ));

        assertThat(sessions).singleElement()
                .satisfies(session -> assertThat(session.getDuration()).isEqualTo(Duration.ofHours(8)));
        assertThat(sessions.getFirst().getSleepDuration()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void assignSleepSessionsToDaysUsesDayWithLargestShare() {
        SleepSession session = new SleepSession(time(22, 0), timeNextDay(7, 0));
        session.setSleepDuration(Duration.ofHours(9));
        DayAnalytics day1 = new DayAnalytics();
        day1.setDate(LocalDate.of(2026, 4, 10));
        DayAnalytics day2 = new DayAnalytics();
        day2.setDate(LocalDate.of(2026, 4, 11));
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(day1.getDate(), day1);
        days.put(day2.getDate(), day2);

        sleepSessionsService.assignSleepSessionsToDays(List.of(session), days);

        assertThat(day1.getSleepSessions()).isEmpty();
        assertThat(day2.getSleepSessions()).containsExactly(session);
    }

    @Test
    void assignSleepSessionsToDaysSkipsMissingTargetDay() {
        SleepSession session = new SleepSession(time(23, 0), timeNextDay(6, 0));
        session.setSleepDuration(Duration.ofHours(7));
        DayAnalytics unrelated = new DayAnalytics();
        unrelated.setDate(LocalDate.of(2026, 4, 15));

        sleepSessionsService.assignSleepSessionsToDays(List.of(session), Map.of(unrelated.getDate(), unrelated));

        assertThat(unrelated.getSleepSessions()).isEmpty();
    }

    @Test
    void sleepSessionExtendDoesNotShrinkEnd() {
        SleepSession session = new SleepSession(time(22, 0), timeNextDay(6, 0));

        session.extend(timeNextDay(5, 0));

        assertThat(session.getEnd()).isEqualTo(timeNextDay(6, 0));
    }

    @Test
    void assignSleepSessionsToDaysUsesStartDayWhenDurationsAreEqual() {
        SleepSession session = new SleepSession(time(21, 0), timeNextDay(3, 0));
        session.setSleepDuration(Duration.ofHours(6));
        DayAnalytics first = new DayAnalytics();
        first.setDate(LocalDate.of(2026, 4, 10));
        DayAnalytics second = new DayAnalytics();
        second.setDate(LocalDate.of(2026, 4, 11));
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(first.getDate(), first);
        days.put(second.getDate(), second);

        sleepSessionsService.assignSleepSessionsToDays(List.of(session), days);

        assertThat(first.getSleepSessions()).isEmpty();
        assertThat(second.getSleepSessions()).containsExactly(session);
    }

    @Test
    void assignSleepSessionsToDaysHandlesSingleDaySession() {
        SleepSession session = new SleepSession(time(1, 0), time(7, 0));
        session.setSleepDuration(Duration.ofHours(6));
        DayAnalytics day = new DayAnalytics();
        day.setDate(LocalDate.of(2026, 4, 10));

        sleepSessionsService.assignSleepSessionsToDays(List.of(session), Map.of(day.getDate(), day));

        assertThat(day.getSleepSessions()).containsExactly(session);
    }

    private ClassifiedRecord record(long id, int startHour, int endHour, ActivityClass activityClass, CategoryCode categoryCode) {
        return new ClassifiedRecord(id, time(startHour, 0), time(endHour, 0), activityClass, categoryCode);
    }

    private ClassifiedRecord record(long id, int startHour, int startMinute, int endHour, int endMinute, ActivityClass activityClass, CategoryCode categoryCode) {
        ZonedDateTime start = time(startHour, startMinute);
        ZonedDateTime end = endHour < startHour || (endHour == startHour && endMinute < startMinute)
                ? ZonedDateTime.of(start.toLocalDate().plusDays(1), java.time.LocalTime.of(endHour, endMinute), ZONE)
                : ZonedDateTime.of(start.toLocalDate(), java.time.LocalTime.of(endHour, endMinute), ZONE);
        return new ClassifiedRecord(id, start, end, activityClass, categoryCode);
    }

    private ZonedDateTime time(int hour, int minute) {
        return ZonedDateTime.of(2026, 4, 10, hour, minute, 0, 0, ZONE);
    }

    private ZonedDateTime timeNextDay(int hour, int minute) {
        return ZonedDateTime.of(2026, 4, 11, hour, minute, 0, 0, ZONE);
    }
}
