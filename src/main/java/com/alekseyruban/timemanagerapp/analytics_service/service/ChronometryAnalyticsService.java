package com.alekseyruban.timemanagerapp.analytics_service.service;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.ActivityRecordSnapshotDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.AnalyticsDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.ChronometryAnalyticsDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.GetChronometryDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.ChronometryCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.client.ActivityServiceClient;
import com.alekseyruban.timemanagerapp.analytics_service.entity.ChronometryAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.entity.User;
import com.alekseyruban.timemanagerapp.analytics_service.exception.ExceptionFactory;
import com.alekseyruban.timemanagerapp.analytics_service.repository.ChronometryAnalyticsRepository;
import com.alekseyruban.timemanagerapp.analytics_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChronometryAnalyticsService {

    private final ChronometryAnalyticsRepository chronometryAnalyticsRepository;
    private final UserRepository userRepository;
    private final ExceptionFactory exceptionFactory;
    private final ActivityServiceClient activityServiceClient;

    public void generateAnalytics(ChronometryCreatedEvent event) {
        ChronometryAnalyticsDto dto = activityServiceClient.getChronometry(
                new GetChronometryDto(event.getChronometryId())
        );

        User user = userRepository.findByDomainId(dto.getUserId())
                .orElseThrow(exceptionFactory::userNotFountException);

        ChronometryAnalytics chronometryAnalytics;

        List<ActivityRecordSnapshotDto> records =
                dto.getActivityRecordSnapshotList();

        if (records == null || records.isEmpty()) {
            chronometryAnalytics = ChronometryAnalytics.builder()
                    .user(user)
                    .chronometryId(dto.getId())
                    .activityStartStdDev(0.0)
                    .regularityScore(0.0)
                    .fragmentationIndex(0.0)
                    .longestSessionRatio(0.0)
                    .build();
        }

        // 1️⃣ Длительности сессий (в минутах)
        List<Long> durations = records.stream()
                .map(r -> Duration.between(r.getStartedAt(), r.getEndedAt()).toMinutes())
                .filter(d -> d > 0)
                .toList();

        long totalMinutes = durations.stream().mapToLong(Long::longValue).sum();
        long maxSession = durations.stream().mapToLong(Long::longValue).max().orElse(0);

        // 2️⃣ Время старта в минутах от начала дня
        List<Integer> startMinutes = records.stream()
                .map(r -> r.getStartedAt()
                        .atZone(ZoneId.of(r.getTimeZone()))
                        .toLocalTime())
                .map(t -> t.getHour() * 60 + t.getMinute())
                .toList();

        // 3️⃣ activityStartStdDev
        double meanStart = startMinutes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        double variance = startMinutes.stream()
                .mapToDouble(m -> Math.pow(m - meanStart, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        // 4️⃣ regularityScore (чем меньше stdDev — тем лучше)
        double regularityScore = Math.max(0, 1 - (stdDev / 720)); // 12 часов как максимум

        // 5️⃣ fragmentationIndex
        double fragmentationIndex =
                (double) durations.size() / Math.max(1, totalMinutes / 60.0);

        // 6️⃣ longestSessionRatio
        double longestSessionRatio =
                totalMinutes == 0 ? 0 : (double) maxSession / totalMinutes;

        chronometryAnalytics = ChronometryAnalytics.builder()
                .user(user)
                .chronometryId(dto.getId())
                .activityStartStdDev(stdDev)
                .regularityScore(regularityScore)
                .fragmentationIndex(fragmentationIndex)
                .longestSessionRatio(longestSessionRatio)
                .build();

        chronometryAnalyticsRepository.save(chronometryAnalytics);
    }

    public AnalyticsDto getAnalyticsOfChronometre(GetChronometryDto dto) {
        ChronometryAnalytics analyticsDto = chronometryAnalyticsRepository.findByChronometryId(dto.getId())
                .orElseThrow(exceptionFactory::chronometryNotFoundException);
        return AnalyticsDto.fromEntity(analyticsDto);
    }

}
