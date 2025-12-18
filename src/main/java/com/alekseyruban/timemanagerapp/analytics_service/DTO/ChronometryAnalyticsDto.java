package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ChronometryAnalyticsDto {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timeZone;
    private List<CategorySnapshotDto> categorySnapshotList;
    private List<ActivitySnapshotDto> activitySnapshotList;
    private List<ActivityVariationSnapshotDto> activityVariationSnapshotList;
    private List<ActivityRecordSnapshotDto> activityRecordSnapshotList;
}