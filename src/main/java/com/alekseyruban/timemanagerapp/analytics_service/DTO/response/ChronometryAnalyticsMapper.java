package com.alekseyruban.timemanagerapp.analytics_service.DTO.response;

import com.alekseyruban.timemanagerapp.analytics_service.entity.AnalyticsIssueEntity;
import com.alekseyruban.timemanagerapp.analytics_service.entity.ChronometryAnalytics;
import com.alekseyruban.timemanagerapp.analytics_service.entity.DayAnalyticsEntity;
import org.springframework.stereotype.Component;

@Component
public class ChronometryAnalyticsMapper {

    public ChronometryAnalyticsDto toDto(ChronometryAnalytics entity) {
        return ChronometryAnalyticsDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .chronometryId(entity.getChronometryId())
                .days(entity.getDays()
                        .stream()
                        .map(this::toDto)
                        .toList())
                .issues(entity.getIssues().stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private DayAnalyticsDto toDto(DayAnalyticsEntity entity) {
        return DayAnalyticsDto.builder()
                .id(entity.getId())
                .chronometryId(entity.getChronometry().getChronometryId())
                .date(entity.getDate())
                .workTime(entity.getWorkTime().toMinutes())
                .leisureTime(entity.getLeisureTime().toMinutes())
                .restTime(entity.getRestTime().toMinutes())
                .isWorkDay(entity.getIsWorkDay())
                .workDayDuration(entity.getWorkDayDuration().toMinutes())
                .focusScore(entity.getFocusScore())
                .issues(entity.getIssues().stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private AnalyticsIssueDto toDto(AnalyticsIssueEntity entity) {
        return AnalyticsIssueDto.builder()
                .id(entity.getId())
                .dayId(entity.getDayAnalytics() != null ? entity.getDayAnalytics().getId() : null)
                .chronometryId(entity.getChronometry() != null ? entity.getChronometry().getId() : null)
                .code(entity.getCode())
                .severity(entity.getSeverity())
                .params(entity.getParams())
                .recommendation(entity.getRecommendation())
                .build();
    }
}