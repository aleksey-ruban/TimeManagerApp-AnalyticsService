package com.alekseyruban.timemanagerapp.analytics_service.service.rabbit;

import com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit.UserCreatedEvent;
import com.alekseyruban.timemanagerapp.analytics_service.config.RabbitConfig;
import com.alekseyruban.timemanagerapp.analytics_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventListener {

    private final UserService userService;

    @RabbitListener(queues = RabbitConfig.USER_CREATED_QUEUE)
    public void onUserCreated(UserCreatedEvent event) {
        userService.createUser(event);
    }
}
