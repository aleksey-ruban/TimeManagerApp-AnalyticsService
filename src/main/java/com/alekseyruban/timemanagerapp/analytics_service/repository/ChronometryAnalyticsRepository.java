package com.alekseyruban.timemanagerapp.analytics_service.repository;

import com.alekseyruban.timemanagerapp.analytics_service.entity.ChronometryAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChronometryAnalyticsRepository extends JpaRepository<ChronometryAnalytics, Long> {
    Optional<ChronometryAnalytics> findByChronometryId(Long chronometryId);
}
