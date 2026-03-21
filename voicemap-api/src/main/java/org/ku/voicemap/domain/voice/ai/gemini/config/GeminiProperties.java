package org.ku.voicemap.domain.voice.ai.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
    String apiKey,
    Urls urls,
    Models models,
    SystemInstructions instructions
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

    public record SystemInstructions(
        String agent,
        String document,
        String chat
    ) {}
}
