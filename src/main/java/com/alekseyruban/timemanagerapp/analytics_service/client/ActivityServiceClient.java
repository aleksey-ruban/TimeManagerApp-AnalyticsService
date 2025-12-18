package com.alekseyruban.timemanagerapp.analytics_service.client;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.ChronometryAnalyticsDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.GetChronometryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "activity-service")
public interface ActivityServiceClient {

    @PostMapping("/internal/chronometry")
    ChronometryAnalyticsDto getChronometry(
            @RequestBody GetChronometryDto dto
    );
}