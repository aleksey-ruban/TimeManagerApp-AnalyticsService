package com.alekseyruban.timemanagerapp.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "user_domain_id")
    private User user;

    @Column(nullable = false)
    private Long chronometryId;

    @Column
    private Double activityStartStdDev;

    @Column
    private Double regularityScore;

    @Column
    private Double fragmentationIndex;

    @Column
    private Double longestSessionRatio;
}
