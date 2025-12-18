package com.alekseyruban.timemanagerapp.analytics_service.controller;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.AnalyticsDto;
import com.alekseyruban.timemanagerapp.analytics_service.DTO.GetChronometryDto;
import com.alekseyruban.timemanagerapp.analytics_service.service.ChronometryAnalyticsService;
import com.alekseyruban.timemanagerapp.analytics_service.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analytics/analytic")
public class analyticsController {

    private final ChronometryAnalyticsService chronometryAnalyticsService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnalyticsDto>> getAnalytics(
            @RequestBody GetChronometryDto dto
    ) {
        AnalyticsDto analytics = chronometryAnalyticsService.getAnalyticsOfChronometre(dto);

        ApiResponse<AnalyticsDto> response = new ApiResponse<>("Analytics created", analytics);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
