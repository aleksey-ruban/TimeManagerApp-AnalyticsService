package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ChronometryDto {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timeZone;
    private List<CategorySnapshotDto> categorySnapshotList;
    private List<ActivitySnapshotDto> activitySnapshotList;
    private List<ActivityVariationSnapshotDto> activityVariationSnapshotList;
    private List<ActivityRecordSnapshotDto> activityRecordSnapshotList;

    public CategorySnapshotDto findCategoryByActivity(ActivitySnapshotDto activity) {
        if (activity.getCategorySnapshotId() == null) {
            return null;
        }

        return categorySnapshotList.stream()
                .filter(c -> c.getId().equals(activity.getCategorySnapshotId()))
                .findFirst()
                .orElse(null);
    }

    public ActivitySnapshotDto findActivityById(Long id) {

        return activitySnapshotList.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}