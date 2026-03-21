package org.ku.voicemap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voicemap.ai")
public record AiInstructionProperties(
    Instructions instructions
) {

    public record Instructions(
        String agent,
        String document,
        String chat,
        String contextSummarizer
    ) {}
}
