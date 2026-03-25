package com.alekseyruban.timemanagerapp.analytics_service.DTO.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChronometryAnalyticsDto {

    private Long id;
    private Long userId;
    private Long chronometryId;

    private List<DayAnalyticsDto> days;
    private List<AnalyticsIssueDto> issues;
}