package org.ku.voicemap.domain.voice.ai.gemini.payload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
    String systemInstruction,
    String apiKey
) {
}
