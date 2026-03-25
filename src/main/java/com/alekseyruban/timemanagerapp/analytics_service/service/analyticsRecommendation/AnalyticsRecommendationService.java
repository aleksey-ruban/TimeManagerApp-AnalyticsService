package com.alekseyruban.timemanagerapp.analytics_service.service.analyticsRecommendation;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsRecommendationService {

    private final RecommendationLLMService recommendationLLM;

    public void enrichRecommendations(
            List<AnalyticsIssue> systemicIssues,
            List<AnalyticsIssue> nonSystemicIssues
    ) {
        Set<IssueCode> systemicCodes = systemicIssues.stream()
                .map(AnalyticsIssue::getCode)
                .collect(Collectors.toSet());


        for (AnalyticsIssue issue : systemicIssues) {
            String recommendation = recommendationLLM.generateSystemic(issue);
            issue.setRecommendation(recommendation);
        }

        Map<IssueCode, List<AnalyticsIssue>> groupedNonSystemic = nonSystemicIssues.stream()
                .filter(issue -> !systemicCodes.contains(issue.getCode()))
                .collect(Collectors.groupingBy(AnalyticsIssue::getCode));

        for (Map.Entry<IssueCode, List<AnalyticsIssue>> entry : groupedNonSystemic.entrySet()) {
            List<AnalyticsIssue> issues = entry.getValue();

            String recommendation = recommendationLLM.generateNonSystemic(issues);

            for (AnalyticsIssue issue : issues) {
                issue.setRecommendation(recommendation);
            }
        }
    }
}
