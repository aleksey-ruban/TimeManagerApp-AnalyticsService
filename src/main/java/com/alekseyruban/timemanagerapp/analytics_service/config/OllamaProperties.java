package com.alekseyruban.timemanagerapp.analytics_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {
    private String host;
    private int port;
    private String model;
    private String keepAlive;
}