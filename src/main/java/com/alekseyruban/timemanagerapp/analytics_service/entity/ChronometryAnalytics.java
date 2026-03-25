package com.alekseyruban.timemanagerapp.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChronometryAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Long chronometryId;

    @OneToMany(mappedBy = "chronometry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DayAnalyticsEntity> days = new ArrayList<>();

    @OneToMany(mappedBy = "chronometry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalyticsIssueEntity> issues = new ArrayList<>();
}
