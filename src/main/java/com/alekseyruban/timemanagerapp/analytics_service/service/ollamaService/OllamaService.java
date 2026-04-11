package com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService;

import com.alekseyruban.timemanagerapp.analytics_service.config.OllamaProperties;
import com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.model.OllamaRequest;
import com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService.model.OllamaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
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

        String url = buildGenerateUrl(properties.getHost(), properties.getPort());

        ResponseEntity<OllamaResponse> response =
                restTemplate.postForEntity(url, request, OllamaResponse.class);

        return response.getBody().getResponse();
    }

    static String buildGenerateUrl(String host, int port) {
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException("Property 'ollama.host' must not be blank");
        }

        String rawHost = host.trim();
        String normalizedHost = hasScheme(rawHost) ? rawHost : "http://" + rawHost;

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(normalizedHost);

        if (builder.build().getPort() == -1 && port > 0) {
            builder.port(port);
        }

        return builder.path("/api/generate").toUriString();
    }

    private static boolean hasScheme(String value) {
        int schemeSeparator = value.indexOf("://");
        return schemeSeparator > 0;
    }
}
