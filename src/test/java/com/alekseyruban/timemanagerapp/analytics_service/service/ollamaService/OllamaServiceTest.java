package com.alekseyruban.timemanagerapp.analytics_service.service.ollamaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaServiceTest {

    @Test
    void buildGenerateUrlAddsHttpSchemeForPlainHostnames() {
        assertEquals(
                "http://ollama:11434/api/generate",
                OllamaService.buildGenerateUrl("ollama", 11434)
        );
    }

    @Test
    void buildGenerateUrlKeepsExplicitSchemeAndPort() {
        assertEquals(
                "http://localhost:11434/api/generate",
                OllamaService.buildGenerateUrl("http://localhost:11434", 11434)
        );
    }
}
