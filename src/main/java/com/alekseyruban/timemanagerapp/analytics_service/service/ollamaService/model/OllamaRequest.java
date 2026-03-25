package com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OllamaRequest {
    private String model;
    private String prompt;

    @JsonProperty("keep_alive")
    private String keepAlive;

    private boolean stream;
}