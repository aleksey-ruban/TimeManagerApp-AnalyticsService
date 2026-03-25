package com.alekseyruban.timemanagerapp.analytics_service.service.analyticsRecommendation.model;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PromptDefinition {
    String problemText;
    String normalValue;
    String recommendationMeaning;
    List<IssueParam> parameterNames;
}