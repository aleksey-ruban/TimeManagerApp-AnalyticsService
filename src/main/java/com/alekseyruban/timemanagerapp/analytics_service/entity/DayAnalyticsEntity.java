package com.alekseyruban.timemanagerapp.analytics_service.entity;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.DayAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.utils.DurationConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayAnalyticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne
    private ChronometryAnalytics chronometry;

    @OneToMany(mappedBy = "dayAnalytics", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalyticsIssueEntity> issues = new ArrayList<>();

    @Convert(converter = DurationConverter.class)
    private Duration workTime = Duration.ZERO;
    @Convert(converter = DurationConverter.class)
    private Duration leisureTime = Duration.ZERO;
    @Convert(converter = DurationConverter.class)
    private Duration restTime = Duration.ZERO;

    private Boolean isWorkDay = false;
    @Convert(converter = DurationConverter.class)
    private Duration workDayDuration = Duration.ZERO;
    private Double focusScore = 0.0;

    public static DayAnalyticsEntity fromDomain(
            DayAnalytics day,
            ChronometryAnalytics chronometry
    ) {
        DayAnalyticsEntity entity = DayAnalyticsEntity.builder()
                .date(day.getDate())
                .chronometry(chronometry)
                .workTime(day.getWorkTime())
                .leisureTime(day.getLeisureTime())
                .restTime(day.getRestTime())
                .isWorkDay(day.getIsWorkDay())
                .workDayDuration(day.getWorkDayDuration())
                .focusScore(day.getFocusScore())
                .build();

        List<AnalyticsIssueEntity> issueEntities = day.getIssues().stream()
                .map(issue -> AnalyticsIssueEntity.fromDomain(issue, entity, null))
                .toList();

        entity.setIssues(issueEntities);

        return entity;
    }
}