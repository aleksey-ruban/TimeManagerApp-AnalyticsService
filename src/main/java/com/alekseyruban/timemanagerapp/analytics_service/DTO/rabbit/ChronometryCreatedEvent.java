package com.alekseyruban.timemanagerapp.analytics_service.DTO.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChronometryCreatedEvent {
    private Long chronometryId;
}
