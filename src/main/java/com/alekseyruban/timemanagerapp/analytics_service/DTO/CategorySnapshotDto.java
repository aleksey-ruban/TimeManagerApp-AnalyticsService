package com.alekseyruban.timemanagerapp.analytics_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySnapshotDto {
    private Long id;
    private String baseName;
    private Long globalCategoryId;
    private CategoryCode code;
}
