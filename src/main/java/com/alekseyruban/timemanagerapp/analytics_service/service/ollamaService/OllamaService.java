package com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService;

import com.alekseyruban.timemanagerapp.analytics_service.config.OllamaProperties;
import com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.model.OllamaRequest;
import com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.model.OllamaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final OllamaProperties properties;

    public OllamaService(OllamaProperties properties) {
        this.properties = properties;
    }

    public String ask(String prompt) {
        OllamaRequest request = new OllamaRequest();
        request.setModel(properties.getModel());
        request.setPrompt(prompt);
        request.setKeepAlive(properties.getKeepAlive());
        request.setStream(false);

        String url = properties.getHost() + ":" + properties.getPort() + "/api/generate";

        ResponseEntity<OllamaResponse> response =
                restTemplate.postForEntity(url, request, OllamaResponse.class);

        return response.getBody().getResponse();
    }
}