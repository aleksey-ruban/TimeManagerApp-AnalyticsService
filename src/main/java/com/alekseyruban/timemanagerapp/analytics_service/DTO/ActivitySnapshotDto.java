package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivitySnapshotDto {
    private Long id;
    private String name;
    private Long categorySnapshotId;
    private String icon;
    private String iconColor;
}
