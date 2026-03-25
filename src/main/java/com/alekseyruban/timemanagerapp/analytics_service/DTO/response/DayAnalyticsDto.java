package com.alekseyruban.timemanagerapp.analytics_service.DTO.response;

import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayAnalyticsDto {

    private Long id;
    private Long chronometryId;
    private LocalDate date;

    private Long workTime;
    private Long leisureTime;
    private Long restTime;

    private Boolean isWorkDay;
    private Long workDayDuration;
    private Double focusScore;

    private List<AnalyticsIssueDto> issues;
}