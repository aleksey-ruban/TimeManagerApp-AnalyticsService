package com.alekseyruban.timemanagerapp.analytics_service.service.rabbit;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.ChronometryCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.config.RabbitConfig;
import com.alekseyruban.timemanagerapp.analytics_service.service.ChronometryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChronometryEventListener {

    private final ChronometryAnalyticsService chronometryAnalyticsService;

    @RabbitListener(queues = RabbitConfig.CHRONOMETRY_CREATED_QUEUE)
    public void onChronometryCreated(ChronometryCreatedEvent event) {
        chronometryAnalyticsService.generateAnalytics(event);
    }
}