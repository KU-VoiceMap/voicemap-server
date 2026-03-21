package org.ku.voicemap.ai.gemini.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record SetupMessage(Setup setup) {

    public static SetupMessage create(String model, String systemInstruction) {
        return create(model, systemInstruction, null);
    }

    public static SetupMessage create(String model, String systemInstruction, String resumptionHandle) {
        return new SetupMessage(new Setup(
            model,
            new SystemInstruction(List.of(new Part(systemInstruction))),
            new GenerationConfig(List.of("AUDIO")),
            new RealtimeInputConfig(new AutomaticActivityDetection(false, 100, 500)),
            new AudioTranscriptionConfig(),
            new AudioTranscriptionConfig(),
            new ContextWindowCompressionConfig(new SlidingWindow()),
            new SessionResumptionConfig(resumptionHandle)
        ));
    }

    private record Setup(
        String model,
        SystemInstruction systemInstruction,
        GenerationConfig generationConfig,
        RealtimeInputConfig realtimeInputConfig,
        AudioTranscriptionConfig inputAudioTranscription,
        AudioTranscriptionConfig outputAudioTranscription,
        ContextWindowCompressionConfig contextWindowCompression,
        SessionResumptionConfig sessionResumption
    ) {}
    private record SystemInstruction(List<Part> parts) {}
    private record Part(String text) {}
    private record GenerationConfig(List<String> responseModalities) {}
    private record RealtimeInputConfig(AutomaticActivityDetection automaticActivityDetection) {}
    private record AutomaticActivityDetection(boolean disabled, long prefixPaddingMs, long silenceDurationMs) {}
    private record AudioTranscriptionConfig() {}
    private record ContextWindowCompressionConfig(SlidingWindow slidingWindow) {}
    private record SlidingWindow() {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SessionResumptionConfig(String handle) {}
}
