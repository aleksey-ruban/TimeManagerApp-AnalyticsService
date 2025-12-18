package com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreatedEvent {
    private Long userId;
}
