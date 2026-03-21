package org.ku.voicemap.ai.gemini.payload;

import java.util.List;
import java.util.Map;

public record GeminiChatRequest(
    SystemInstruction systemInstruction,
    List<Content> contents,
    GenerationConfig generationConfig
) {

    public static GeminiChatRequest of(
        String systemInstruction,
        String prompt,
        Map<String, Object> responseSchema
    ) {
        return new GeminiChatRequest(
            new SystemInstruction(List.of(new Part(systemInstruction))),
            List.of(new Content(List.of(new Part(prompt)))),
            new GenerationConfig("application/json", responseSchema)
        );
    }

    record SystemInstruction(List<Part> parts) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
    record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {}
}
