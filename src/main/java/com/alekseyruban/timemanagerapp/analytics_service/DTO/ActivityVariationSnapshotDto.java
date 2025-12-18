package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivityVariationSnapshotDto {
    private Long id;
    private String value;
    private Long activitySnapshotId;
}
