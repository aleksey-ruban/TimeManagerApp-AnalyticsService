package com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.*;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.ChronometryCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.response.ChronometryAnalyticsDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.response.ChronometryAnalyticsMapper;
import com.alekseyruban.timemanagerapp.analytics_service.client.ActivityServiceClient;
import com.alekseyruban.timemanagerapp.analytics_service.entity.AnalyticsIssueEntity;
import com.alekseyruban.timemanagerapp.analytics_service.entity.ChronometryAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.entity.DayAnalyticsEntity;
import com.alekseyruban.timemanagerapp.analytics_service.entity.User;
import com.alekseyruban.timemanagerapp.analytics_service.exception.ExceptionFactory;
import com.alekseyruban.timemanagerapp.analytics_service.repository.ChronometryAnalyticsRepository;
import com.alekseyruban.timemanagerapp.analytics_service.repository.UserRepository;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClass;
import com.alekseyruban.timemanagerapp.analytics_service.service.activityClassifier.ActivityClassifierService;
import com.alekseyruban.timemanagerapp.analytics_service.service.analyticsRecommendation.AnalyticsRecommendationService;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.detectServices.*;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.*;
import com.alekseyruban.timemanagerapp.analytics_service.utils.RetryOptimisticLock;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChronometryAnalyticsService {

    private final ChronometryAnalyticsRepository chronometryAnalyticsRepository;
    private final UserRepository userRepository;
    private final ExceptionFactory exceptionFactory;
    private final ActivityServiceClient activityServiceClient;
    private final ActivityClassifierService activityClassifierService;
    private final AnalyticsRecommendationService analyticsRecommendationService;
    private final ChronometryAnalyticsMapper chronometryAnalyticsMapper;

    private final InsufficientSleepService insufficientSleepService;
    private final LongWorkdayService longWorkdayService;
    private final FragmentedWorkdayService fragmentedWorkdayService;
    private final MultitaskingIssueService multitaskingIssueService;
    private final ManyTaskSwitchesService manyTaskSwitchesService;
    private final NoBreaksService noBreaksService;
    private final LowFocusService lowFocusService;
    private final WorkSessionsService workSessionsService;
    private final SleepSessionsService sleepSessionsService;
    private final IrregularSleepService irregularSleepService;
    private final NoDaysOffService noDaysOffService;
    private final WeeklyIssuesService weeklyIssuesService;

    @RetryOptimisticLock
    @Transactional
    public void generateAnalytics(ChronometryCreatedEvent event) {
        ChronometryDto chronometryDto = activityServiceClient.getChronometry(
                new GetChronometryDto(event.getChronometryId())
        );
        ZoneId chronometryZone = ZoneId.of(chronometryDto.getTimeZone());

        User user = userRepository.findByDomainId(chronometryDto.getUserId())
                .orElseThrow(exceptionFactory::userNotFountException);


        Map<Long, CategorySnapshotDto> categoryIndex = new HashMap<>();
        for (CategorySnapshotDto category : chronometryDto.getCategorySnapshotList()) {
            categoryIndex.putIfAbsent(category.getId(), category);
        }

        Map<Long, ActivitySnapshotDto> activityIndex = new HashMap<>();
        for (ActivitySnapshotDto activity : chronometryDto.getActivitySnapshotList()) {
            ActivityClass activityClass = activityClassifierService.classifyActivity(
                    activity,
                    chronometryDto.findCategoryByActivity(activity)
            );
            activity.setActivityClass(activityClass);

            activityIndex.putIfAbsent(activity.getId(), activity);
        }

        List<ClassifiedRecord> records = new ArrayList<>(chronometryDto.getActivityRecordSnapshotList()
                .stream()
                .map(r -> {
                    ActivitySnapshotDto activity = activityIndex.get(r.getActivitySnapshotId());
                    CategorySnapshotDto category = categoryIndex.get(activity.getCategorySnapshotId());
                    return new ClassifiedRecord(
                            r.getId(),
                            r.getStartedAt().atZone(chronometryZone),
                            r.getEndedAt().atZone(chronometryZone),
                            activity.getActivityClass(),
                            category != null ? category.getCode() : null
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new)));

        records.sort(Comparator.comparing(ClassifiedRecord::getStart));

        List<SleepSession> sleepSessions = sleepSessionsService.buildSleepSessions(records);

        Map<LocalDate, DayAnalytics> days = new LinkedHashMap<>();
        LocalDate current = chronometryDto.getStartDate();

        for (int i = 0; i < 7; i++) {
            DayAnalytics dayAnalytics = new DayAnalytics();
            dayAnalytics.setDate(current);
            days.put(current, dayAnalytics);
            current = current.plusDays(1);
        }

        sleepSessionsService.assignSleepSessionsToDays(sleepSessions, days);

        for (ClassifiedRecord record : records) {
            ZonedDateTime start = record.getStart();
            ZonedDateTime end = record.getEnd();

            while (!start.toLocalDate().equals(end.toLocalDate())) {
                if (start.toLocalDate().isBefore(chronometryDto.getStartDate()) || start.toLocalDate().isAfter(chronometryDto.getEndDate())) {
                    break;
                }

                ZonedDateTime midnight = start.toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(chronometryZone);

                ClassifiedRecord part = new ClassifiedRecord(
                        record.getId(),
                        start,
                        midnight,
                        record.getActivityClass(),
                        record.getCategoryCode()
                );
                days.get(start.toLocalDate()).getRecords().add(part);

                start = midnight;
            }

            if (!start.toLocalDate().isBefore(chronometryDto.getStartDate()) && !start.toLocalDate().isAfter(chronometryDto.getEndDate())) {
                ClassifiedRecord lastPart = new ClassifiedRecord(
                        record.getId(),
                        start,
                        end,
                        record.getActivityClass(),
                        record.getCategoryCode()
                );

                days.get(start.toLocalDate()).getRecords().add(lastPart);
            }
        }

        for (DayAnalytics day : days.values()) {

            List<ClassifiedRecord> dayRecords = day.getRecords();

            List<TimePoint> points = new ArrayList<>();

            for (ClassifiedRecord r : dayRecords) {
                points.add(new TimePoint(r.getId(), r.getStart(), true, r.getActivityClass()));
                points.add(new TimePoint(r.getId(), r.getEnd(), false, r.getActivityClass()));
            }

            points.sort(Comparator.comparing(p -> p.getTime()));

            Map<ActivityClass, Duration> durations = new EnumMap<>(ActivityClass.class);
            Map<ActivityClass, Integer> activeClasses = new HashMap<>();

            for (int i = 0; i < points.size() - 1; i++) {

                TimePoint p = points.get(i);

                if (p.isStart()) {
                    activeClasses.put(
                            p.getActivityClass(),
                            activeClasses.getOrDefault(p.getActivityClass(), 0) + 1
                    );
                } else {
                    activeClasses.computeIfPresent(p.getActivityClass(), (k, v) -> {
                        if (v == 1) return null;
                        return v - 1;
                    });
                }

                ZonedDateTime nextTime = points.get(i + 1).getTime();
                Duration delta = Duration.between(p.getTime(), nextTime);

                if (delta.isZero()) continue;

                ActivityClass dominant = resolveDominantClass(activeClasses);

                if (dominant != null) {
                    durations.merge(dominant, delta, Duration::plus);
                }
            }

            day.setWorkTime(durations.getOrDefault(ActivityClass.WORK, Duration.ZERO));
            day.setLeisureTime(durations.getOrDefault(ActivityClass.LEISURE, Duration.ZERO));
            day.setRestTime(durations.getOrDefault(ActivityClass.REST, Duration.ZERO));

            day.setIsWorkDay(day.getWorkTime().toMinutes() > 180);

            Optional<AnalyticsIssue> insufficientSleep = insufficientSleepService.detectInsufficientSleep(day);
            insufficientSleep.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            if (!day.getIsWorkDay()) {
                continue;
            }

            List<WorkSession> workSessions = workSessionsService.buildWorkSessions(day.getRecords());

            Duration workDayDuration = workSessions.stream()
                    .map(WorkSession::getDuration)
                    .reduce(Duration.ZERO, Duration::plus);
            day.setWorkDayDuration(workDayDuration);

            Double focusScore = workDayDuration.isZero() ? 0.0 : (double) day.getWorkTime().toMinutes() / workDayDuration.toMinutes();
            day.setFocusScore(focusScore);

            Optional<AnalyticsIssue> lowFocus = lowFocusService.detectLowFocus(day);
            lowFocus.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            Optional<AnalyticsIssue> noBreaks = noBreaksService.detectNoBreaks(
                    day,
                    points,
                    workSessions
            );
            noBreaks.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            Optional<AnalyticsIssue> taskSwitches = manyTaskSwitchesService.detectManyTaskSwitches(day, workSessions);
            taskSwitches.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            Optional<AnalyticsIssue> multitasking = multitaskingIssueService.detectMultitasking(day, points, workSessions);
            multitasking.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            Optional<AnalyticsIssue> fragmentedWorkday = fragmentedWorkdayService.detectFragmentedWorkday(day, workSessions);
            fragmentedWorkday.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));

            Optional<AnalyticsIssue> longWorkday = longWorkdayService.detectLongWorkday(day);
            longWorkday.ifPresent(analyticsIssue -> day.getIssues().add(analyticsIssue));
        }

        List<AnalyticsIssue> weeklyIssues = weeklyIssuesService.buildWeeklyIssues(days);

        Optional<AnalyticsIssue> noDaysOff = noDaysOffService.buildNoDaysOffIssue(days);
        noDaysOff.ifPresent(weeklyIssues::add);

        Optional<AnalyticsIssue> irregularSleep = irregularSleepService.buildIrregularSleepIssue(days);
        irregularSleep.ifPresent(weeklyIssues::add);


        Set<IssueCode> weeklyCodes = weeklyIssues.stream()
                .map(AnalyticsIssue::getCode)
                .collect(Collectors.toSet());

        List<AnalyticsIssue> nonSystemicIssues = days.values().stream()
                .flatMap(day -> day.getIssues().stream())
                .filter(issue -> !weeklyCodes.contains(issue.getCode()))
                .collect(Collectors.toCollection(ArrayList::new));

        analyticsRecommendationService.enrichRecommendations(
                weeklyIssues,
                nonSystemicIssues
        );


        ChronometryAnalytics chronometry = ChronometryAnalytics.builder()
                .user(user)
                .chronometryId(chronometryDto.getId())
                .build();

        List<DayAnalyticsEntity> dayEntities = days.values().stream()
                .map(day -> DayAnalyticsEntity.fromDomain(day, chronometry))
                .collect(Collectors.toCollection(ArrayList::new));

        chronometry.setDays(dayEntities);

        List<AnalyticsIssueEntity> issueEntities = weeklyIssues.stream()
                .map(issue -> AnalyticsIssueEntity.fromDomain(issue, null, chronometry))
                .collect(Collectors.toCollection(ArrayList::new));

        chronometry.setIssues(issueEntities);

        chronometryAnalyticsRepository.save(chronometry);
    }

    public ChronometryAnalyticsDto getAnalyticsOfChronometry(GetChronometryDto dto) {
        ChronometryAnalytics analyticsDto = chronometryAnalyticsRepository.findByChronometryId(dto.getId())
                .orElseThrow(exceptionFactory::chronometryNotFoundException);
        return chronometryAnalyticsMapper.toDto(analyticsDto);
    }

    private ActivityClass resolveDominantClass(Map<ActivityClass, Integer> active) {

        if (active.getOrDefault(ActivityClass.WORK, 0) > 0) {
            return ActivityClass.WORK;
        }

        if (active.getOrDefault(ActivityClass.LEISURE, 0) > 0) {
            return ActivityClass.LEISURE;
        }

        if (active.getOrDefault(ActivityClass.REST, 0) > 0) {
            return ActivityClass.REST;
        }

        return null;
    }
}