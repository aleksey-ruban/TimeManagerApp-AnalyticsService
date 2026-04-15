package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.CategoryCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.ClassifiedRecord;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.DayAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueParam;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueSeverity;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.SleepSession;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.TimePoint;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.WorkSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IssueDetectionServicesTest {

    private final NoBreaksService noBreaksService = new NoBreaksService();
    private final LowFocusService lowFocusService = new LowFocusService();
    private final LongWorkdayService longWorkdayService = new LongWorkdayService();
    private final ManyTaskSwitchesService manyTaskSwitchesService = new ManyTaskSwitchesService();
    private final FragmentedWorkdayService fragmentedWorkdayService = new FragmentedWorkdayService();
    private final InsufficientSleepService insufficientSleepService = new InsufficientSleepService();
    private final IrregularSleepService irregularSleepService = new IrregularSleepService();
    private final MultitaskingIssueService multitaskingIssueService = new MultitaskingIssueService();
    private final NoDaysOffService noDaysOffService = new NoDaysOffService();
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    @Test
    void detectNoBreaksReturnsEmptyForNonWorkDay() {
        assertThat(noBreaksService.detectNoBreaks(day(false, 4, 240, 0.8), List.of(), List.of())).isEmpty();
    }

    @Test
    void detectNoBreaksReturnsEmptyForShortWorkday() {
        DayAnalytics day = day(true, 1, 90, 0.8);
        assertThat(noBreaksService.detectNoBreaks(day, List.of(), List.of())).isEmpty();
    }

    @Test
    void detectNoBreaksReturnsIssueWhenBreaksPerHourBelowThreshold() {
        DayAnalytics day = day(true, 8, 480, 0.8);
        List<WorkSession> sessions = List.of(new WorkSession(time(9, 0), time(17, 0)));
        List<TimePoint> points = List.of(
                point(1, 9, 0, true), point(1, 12, 0, false),
                point(1, 12, 10, true), point(1, 17, 0, false)
        );

        Optional<AnalyticsIssue> issue = noBreaksService.detectNoBreaks(day, points, sessions);

        assertThat(issue).isPresent();
        assertThat(issue.get().getCode()).isEqualTo(IssueCode.NO_BREAKS);
        assertThat(issue.get().getParams()).containsKey(IssueParam.TOTAL_BREAKS_MINUTES);
    }

    @Test
    void detectNoBreaksStillReturnsIssueWhenBreakPointsExistBecauseBreaksAreNotAccumulated() {
        DayAnalytics day = day(true, 8, 480, 0.8);
        List<WorkSession> sessions = List.of(new WorkSession(time(9, 0), time(17, 0)));
        List<TimePoint> points = List.of(
                point(1, 9, 0, true), point(1, 11, 0, false),
                point(1, 12, 0, true), point(1, 14, 0, false),
                point(1, 15, 0, true), point(1, 17, 0, false)
        );

        assertThat(noBreaksService.detectNoBreaks(day, points, sessions)).isPresent();
    }

    @Test
    void detectLowFocusReturnsEmptyWhenFocusIsAcceptable() {
        assertThat(lowFocusService.detectLowFocus(day(true, 6, 480, 0.7))).isEmpty();
    }

    @Test
    void detectLowFocusReturnsIssueWithLossMinutes() {
        AnalyticsIssue issue = lowFocusService.detectLowFocus(day(true, 4, 480, 0.4)).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.LOW_FOCUS);
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(issue.getParams().get(IssueParam.FOCUS_LOSS_MINUTES)).isEqualTo(72);
    }

    @Test
    void detectLowFocusReturnsEmptyForNonWorkDay() {
        assertThat(lowFocusService.detectLowFocus(day(false, 4, 480, 0.2))).isEmpty();
    }

    @Test
    void detectLongWorkdayReturnsEmptyWhenBelowThreshold() {
        assertThat(longWorkdayService.detectLongWorkday(day(true, 8, 539, 0.7))).isEmpty();
    }

    @Test
    void detectLongWorkdayReturnsIssueWithOvertimeMinutes() {
        AnalyticsIssue issue = longWorkdayService.detectLongWorkday(day(true, 9, 600, 0.7)).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.LONG_WORKDAY);
        assertThat(issue.getParams().get(IssueParam.OVERTIME_MINUTES)).isEqualTo(60L);
    }

    @Test
    void detectLongWorkdayReturnsEmptyForNonWorkDay() {
        assertThat(longWorkdayService.detectLongWorkday(day(false, 10, 700, 0.7))).isEmpty();
    }

    @Test
    void detectManyTaskSwitchesReturnsEmptyForNonWorkDay() {
        DayAnalytics day = day(false, 8, 480, 0.7);
        day.setRecords(List.of(workRecord(1, 9, 0, 9, 20), workRecord(2, 9, 21, 9, 40)));

        assertThat(manyTaskSwitchesService.detectManyTaskSwitches(day, List.of(new WorkSession(time(9, 0), time(10, 0))))).isEmpty();
    }

    @Test
    void detectManyTaskSwitchesReturnsEmptyWhenSwitchRateIsLow() {
        DayAnalytics day = day(true, 8, 480, 0.7);
        day.setRecords(List.of(workRecord(1, 9, 0, 10, 0), workRecord(2, 11, 0, 12, 0)));

        assertThat(manyTaskSwitchesService.detectManyTaskSwitches(day, List.of(new WorkSession(time(9, 0), time(12, 0))))).isEmpty();
    }

    @Test
    void detectManyTaskSwitchesReturnsIssueWhenSwitchRateIsHigh() {
        DayAnalytics day = day(true, 1, 120, 0.7);
        day.setRecords(List.of(
                workRecord(1, 9, 0, 9, 10),
                workRecord(2, 9, 11, 9, 20),
                workRecord(3, 9, 21, 9, 30),
                workRecord(4, 9, 31, 9, 40),
                workRecord(5, 9, 41, 9, 50),
                workRecord(6, 9, 51, 10, 0),
                workRecord(7, 10, 1, 10, 10),
                workRecord(8, 10, 11, 10, 20),
                workRecord(9, 10, 21, 10, 30),
                workRecord(10, 10, 31, 10, 40),
                workRecord(11, 10, 41, 10, 50),
                workRecord(12, 10, 51, 11, 0)
        ));

        AnalyticsIssue issue = manyTaskSwitchesService.detectManyTaskSwitches(
                day,
                List.of(new WorkSession(time(9, 0), time(11, 0)))
        ).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.TOO_MANY_TASK_SWITCHES);
        assertThat(issue.getParams()).containsKeys(IssueParam.TASK_SWITCHES_COUNT, IssueParam.AVG_TIME_PER_TASK);
    }

    @Test
    void detectFragmentedWorkdayReturnsEmptyWhenNotEnoughSessions() {
        assertThat(fragmentedWorkdayService.detectFragmentedWorkday(
                day(true, 5, 300, 0.7),
                List.of(new WorkSession(time(9, 0), time(10, 0)), new WorkSession(time(11, 0), time(12, 0)))
        )).isEmpty();
    }

    @Test
    void detectFragmentedWorkdayReturnsIssueWithAverages() {
        AnalyticsIssue issue = fragmentedWorkdayService.detectFragmentedWorkday(
                day(true, 5, 300, 0.7),
                List.of(
                        new WorkSession(time(9, 0), time(10, 0)),
                        new WorkSession(time(12, 0), time(13, 0)),
                        new WorkSession(time(15, 0), time(16, 0))
                )
        ).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.FRAGMENTED_WORKDAY);
        assertThat(issue.getParams().get(IssueParam.WORK_SESSION_COUNT)).isEqualTo(3);
    }

    @Test
    void detectFragmentedWorkdayReturnsEmptyForNonWorkDay() {
        assertThat(fragmentedWorkdayService.detectFragmentedWorkday(
                day(false, 5, 300, 0.7),
                List.of(
                        new WorkSession(time(9, 0), time(10, 0)),
                        new WorkSession(time(12, 0), time(13, 0)),
                        new WorkSession(time(15, 0), time(16, 0))
                )
        )).isEmpty();
    }

    @Test
    void detectInsufficientSleepReturnsEmptyWhenEnoughSleep() {
        DayAnalytics day = new DayAnalytics();
        day.setDate(LocalDate.of(2026, 4, 10));
        SleepSession session = new SleepSession(time(0, 0), time(8, 0));
        day.setSleepSessions(List.of(session));

        assertThat(insufficientSleepService.detectInsufficientSleep(day)).isEmpty();
    }

    @Test
    void detectInsufficientSleepReturnsIssueWithDeficit() {
        DayAnalytics day = new DayAnalytics();
        day.setDate(LocalDate.of(2026, 4, 10));
        SleepSession session = new SleepSession(time(1, 0), time(6, 0));
        day.setSleepSessions(List.of(session));

        AnalyticsIssue issue = insufficientSleepService.detectInsufficientSleep(day).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.INSUFFICIENT_SLEEP);
        assertThat(issue.getParams().get(IssueParam.SLEEP_DEFICIT_MINUTES)).isEqualTo(120L);
    }

    @Test
    void detectInsufficientSleepReturnsEmptyAtExactThreshold() {
        DayAnalytics day = new DayAnalytics();
        day.setDate(LocalDate.of(2026, 4, 10));
        SleepSession session = new SleepSession(time(0, 0), time(7, 0));
        day.setSleepSessions(List.of(session));

        assertThat(insufficientSleepService.detectInsufficientSleep(day)).isEmpty();
    }

    @Test
    void detectIrregularSleepReturnsEmptyWhenNoSleepSessions() {
        assertThat(irregularSleepService.buildIrregularSleepIssue(new LinkedHashMap<>())).isEmpty();
    }

    @Test
    void detectIrregularSleepReturnsEmptyWhenDeviationDaysAreTooFew() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), sleepDay(LocalDate.of(2026, 4, 1), LocalTime.of(23, 0), 7));
        days.put(LocalDate.of(2026, 4, 2), sleepDay(LocalDate.of(2026, 4, 2), LocalTime.of(23, 15), 7));
        days.put(LocalDate.of(2026, 4, 3), sleepDay(LocalDate.of(2026, 4, 3), LocalTime.of(23, 40), 7));

        assertThat(irregularSleepService.buildIrregularSleepIssue(days)).isEmpty();
    }

    @Test
    void detectIrregularSleepReturnsIssueWhenDeviationIsFrequent() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), sleepDay(LocalDate.of(2026, 4, 1), LocalTime.of(22, 0), 7));
        days.put(LocalDate.of(2026, 4, 2), sleepDay(LocalDate.of(2026, 4, 2), LocalTime.of(1, 30), 6));
        days.put(LocalDate.of(2026, 4, 3), sleepDay(LocalDate.of(2026, 4, 3), LocalTime.of(22, 15), 8));
        days.put(LocalDate.of(2026, 4, 4), sleepDay(LocalDate.of(2026, 4, 4), LocalTime.of(2, 0), 5));
        days.put(LocalDate.of(2026, 4, 5), sleepDay(LocalDate.of(2026, 4, 5), LocalTime.of(21, 50), 8));
        days.put(LocalDate.of(2026, 4, 6), sleepDay(LocalDate.of(2026, 4, 6), LocalTime.of(3, 0), 5));

        AnalyticsIssue issue = irregularSleepService.buildIrregularSleepIssue(days).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.IRREGULAR_SLEEP);
        assertThat(issue.getParams()).containsKeys(IssueParam.SLEEP_START_STD_DEV, IssueParam.WEEKLY_OCCURRENCES);
    }

    @Test
    void detectIrregularSleepSetsPeriodRangeFromFirstToLastDay() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), sleepDay(LocalDate.of(2026, 4, 1), LocalTime.of(22, 0), 7));
        days.put(LocalDate.of(2026, 4, 2), sleepDay(LocalDate.of(2026, 4, 2), LocalTime.of(2, 0), 6));
        days.put(LocalDate.of(2026, 4, 3), sleepDay(LocalDate.of(2026, 4, 3), LocalTime.of(22, 30), 8));
        days.put(LocalDate.of(2026, 4, 4), sleepDay(LocalDate.of(2026, 4, 4), LocalTime.of(3, 0), 5));
        days.put(LocalDate.of(2026, 4, 5), sleepDay(LocalDate.of(2026, 4, 5), LocalTime.of(21, 30), 8));
        days.put(LocalDate.of(2026, 4, 6), sleepDay(LocalDate.of(2026, 4, 6), LocalTime.of(4, 0), 5));

        AnalyticsIssue issue = irregularSleepService.buildIrregularSleepIssue(days).orElseThrow();

        assertThat(issue.getPeriodStart()).isEqualTo(LocalDate.of(2026, 4, 1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        assertThat(issue.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 4, 7).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
    }

    @Test
    void detectMultitaskingReturnsEmptyForNonWorkDay() {
        assertThat(multitaskingIssueService.detectMultitasking(day(false, 8, 480, 0.8), List.of(), List.of())).isEmpty();
    }

    @Test
    void detectMultitaskingReturnsEmptyWhenOverlapRateIsLow() {
        DayAnalytics day = day(true, 8, 480, 0.8);
        List<WorkSession> sessions = List.of(new WorkSession(time(9, 0), time(17, 0)));
        List<TimePoint> points = List.of(
                point(1, 9, 0, true), point(2, 9, 5, true), point(2, 9, 10, false), point(1, 17, 0, false)
        );

        assertThat(multitaskingIssueService.detectMultitasking(day, points, sessions)).isEmpty();
    }

    @Test
    void detectMultitaskingReturnsIssueWhenOverlapRateIsHigh() {
        DayAnalytics day = day(true, 2, 120, 0.8);
        List<WorkSession> sessions = List.of(new WorkSession(time(9, 0), time(11, 0)));
        List<TimePoint> points = List.of(
                point(1, 9, 0, true), point(2, 9, 10, true), point(2, 9, 30, false),
                point(3, 10, 0, true), point(3, 10, 20, false), point(1, 11, 0, false)
        );

        AnalyticsIssue issue = multitaskingIssueService.detectMultitasking(day, points, sessions).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.EXCESSIVE_MULTITASKING);
        assertThat(issue.getParams().get(IssueParam.MULTITASK_OVERLAP_MINUTES)).isEqualTo(40L);
    }

    @Test
    void detectMultitaskingReturnsEmptyWhenWorkTimeIsZero() {
        DayAnalytics day = day(true, 0, 120, 0.8);
        List<WorkSession> sessions = List.of(new WorkSession(time(9, 0), time(11, 0)));
        List<TimePoint> points = List.of(
                point(1, 9, 0, true), point(2, 9, 10, true), point(2, 10, 10, false), point(1, 11, 0, false)
        );

        assertThat(multitaskingIssueService.detectMultitasking(day, points, sessions)).isEmpty();
    }

    @Test
    void detectMultitaskingReturnsEmptyWhenThereAreNoSessions() {
        DayAnalytics day = day(true, 2, 120, 0.8);

        assertThat(multitaskingIssueService.detectMultitasking(day, List.of(), List.of())).isEmpty();
    }

    @Test
    void detectNoDaysOffReturnsEmptyWhenAtLeastTwoDaysOffExist() {
        Map<LocalDate, DayAnalytics> days = Map.of(
                LocalDate.of(2026, 4, 1), day(false, 0, 0, 0.0),
                LocalDate.of(2026, 4, 2), day(false, 0, 0, 0.0),
                LocalDate.of(2026, 4, 3), day(true, 8, 480, 0.7)
        );

        assertThat(noDaysOffService.buildNoDaysOffIssue(days)).isEmpty();
    }

    @Test
    void detectNoDaysOffReturnsIssueWhenRestDaysAreInsufficient() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), day(true, 8, 480, 0.7));
        days.put(LocalDate.of(2026, 4, 2), day(true, 8, 480, 0.7));
        days.put(LocalDate.of(2026, 4, 3), day(true, 8, 480, 0.7));
        days.put(LocalDate.of(2026, 4, 4), day(false, 0, 0, 0.0));

        AnalyticsIssue issue = noDaysOffService.buildNoDaysOffIssue(days).orElseThrow();

        assertThat(issue.getCode()).isEqualTo(IssueCode.NO_DAYS_OFF);
        assertThat(issue.getParams().get(IssueParam.TOTAL_DAYS_OFF_COUNT)).isEqualTo(1L);
    }

    @Test
    void detectNoDaysOffReturnsIssueWhenThereAreNoRestDays() {
        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        days.put(LocalDate.of(2026, 4, 1), day(true, 8, 480, 0.7));
        days.put(LocalDate.of(2026, 4, 2), day(true, 8, 480, 0.7));
        days.put(LocalDate.of(2026, 4, 3), day(true, 8, 480, 0.7));

        AnalyticsIssue issue = noDaysOffService.buildNoDaysOffIssue(days).orElseThrow();

        assertThat(issue.getParams().get(IssueParam.TOTAL_DAYS_OFF_COUNT)).isEqualTo(0L);
        assertThat(issue.getParams().get(IssueParam.TOTAL_WORK_DAYS)).isEqualTo(3L);
    }

    private DayAnalytics day(boolean isWorkDay, long workHours, long workDayMinutes, double focusScore) {
        DayAnalytics day = new DayAnalytics();
        day.setDate(LocalDate.of(2026, 4, 10));
        day.setIsWorkDay(isWorkDay);
        day.setWorkTime(Duration.ofHours(workHours));
        day.setWorkDayDuration(Duration.ofMinutes(workDayMinutes));
        day.setFocusScore(focusScore);
        return day;
    }

    private ClassifiedRecord workRecord(long id, int startHour, int startMinute, int endHour, int endMinute) {
        return new ClassifiedRecord(id, time(startHour, startMinute), time(endHour, endMinute), ActivityClass.WORK, CategoryCode.WORK);
    }

    private TimePoint point(long id, int hour, int minute, boolean isStart) {
        return new TimePoint(id, time(hour, minute), isStart, ActivityClass.WORK);
    }

    private DayAnalytics sleepDay(LocalDate date, LocalTime start, long hours) {
        DayAnalytics day = new DayAnalytics();
        day.setDate(date);
        SleepSession session = new SleepSession(ZonedDateTime.of(date, start, ZONE), ZonedDateTime.of(date, start, ZONE).plusHours(hours));
        session.setSleepDuration(Duration.ofHours(hours));
        day.setSleepSessions(List.of(session));
        return day;
    }

    private ZonedDateTime time(int hour, int minute) {
        return ZonedDateTime.of(2026, 4, 10, hour, minute, 0, 0, ZONE);
    }
}
