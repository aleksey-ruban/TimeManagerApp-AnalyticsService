package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ActivityRecordSnapshotDto {
    private Long id;
    private Long activitySnapshotId;
    private Long variationSnapshotId;
    private Instant startedAt;
    private Instant endedAt;
    private String timeZone;
}
