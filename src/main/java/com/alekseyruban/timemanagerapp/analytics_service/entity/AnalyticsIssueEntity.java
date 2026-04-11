package com.alekseyruban.timemanagerapp.analytics_service.entity;

import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.AnalyticsIssue;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueCode;
import com.alekseyruban.timemanagerapp.analytics_service.service.chronometryAnalytics.model.IssueSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private IssueCode code;

    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> params;

    @ManyToOne
    private ChronometryAnalytics chronometry;

    @ManyToOne
    private DayAnalyticsEntity dayAnalytics;

    @Column(columnDefinition = "text")
    private String recommendation;

    public static AnalyticsIssueEntity fromDomain(
            AnalyticsIssue issue,
            DayAnalyticsEntity dayAnalytics,
            ChronometryAnalytics chronometry
    ) {
        if (issue == null) return null;

        Map<String, Object> params = issue.getParams().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(
                        Collectors.toMap(
                                e -> e.getKey().name(),
                                e -> normalizeParamValue(e.getValue())
                        )
                );

        return AnalyticsIssueEntity.builder()
                .code(issue.getCode())
                .severity(issue.getSeverity())
                .params(params)
                .dayAnalytics(dayAnalytics)
                .chronometry(chronometry)
                .recommendation(issue.getRecommendation())
                .build();
    }

    private static Serializable normalizeParamValue(Object value) {
        if (value instanceof Serializable serializable) {
            if (value instanceof Duration duration) {
                return duration.toMinutes();
            }

            return serializable;
        }

        throw new IllegalArgumentException("Unsupported analytics issue param type: " + value.getClass().getName());
    }
}
