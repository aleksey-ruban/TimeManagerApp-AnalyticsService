package com.alekseyruban.timemanagerapp.analytics_service.DTO.response;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueSeverity;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsIssueDto {

    private Long id;
    private IssueCode code;
    private IssueSeverity severity;

    private Map<String, Object> params;

    private Long chronometryId;
    private Long dayId;

    private String recommendation;
}