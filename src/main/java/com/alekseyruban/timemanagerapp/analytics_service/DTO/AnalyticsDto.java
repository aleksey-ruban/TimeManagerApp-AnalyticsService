package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import com.alekseyruban.timemanagerapp.analytics_service.entity.ChronometryAnalytics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AnalyticsDto {
    private Long id;

    private Long chronometryId;

    private Double activityStartStdDev;

    private Double regularityScore;

    private Double fragmentationIndex;

    private Double longestSessionRatio;

    public static AnalyticsDto fromEntity(ChronometryAnalytics entity) {
        if (entity == null) {
            return null;
        }

        return AnalyticsDto.builder()
                .id(entity.getId())
                .chronometryId(entity.getChronometryId())
                .activityStartStdDev(entity.getActivityStartStdDev())
                .regularityScore(entity.getRegularityScore())
                .fragmentationIndex(entity.getFragmentationIndex())
                .longestSessionRatio(entity.getLongestSessionRatio())
                .build();
    }
}