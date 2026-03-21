package org.ku.voicemap.ai.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
    String apiKey,
    Urls urls,
    Models models
) {
    public record Urls(
        String agentWebSocketUrl,
        String documentApiUrl,
        String chatApiUrl
    ) {}

    public record Models(
        String agentModel,
        String documentModel,
        String chatModel
    ) {}
}
