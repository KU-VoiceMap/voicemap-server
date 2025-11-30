package org.ku.voicemap.domain.voice.ai.gemini.payload;

import java.util.List;

public record SetupMessage(Setup setup) {

    public static SetupMessage create(String systemInstruction) {
        return new SetupMessage(new Setup(
            "models/gemini-2.5-flash-native-audio-preview-09-2025",
            new SystemInstruction(List.of(new Part(systemInstruction))),
            new GenerationConfig(List.of("AUDIO")),
            new RealtimeInputConfig(new AutomaticActivityDetection(false, 100, 500)),
            new AudioTranscriptionConfig(),
            new AudioTranscriptionConfig()
        ));
    }

    private record Setup(
        String model,
        SystemInstruction systemInstruction,
        GenerationConfig generationConfig,
        RealtimeInputConfig realtimeInputConfig,
        AudioTranscriptionConfig inputAudioTranscription,
        AudioTranscriptionConfig outputAudioTranscription
    ) {}
    private record SystemInstruction(List<Part> parts) {}
    private record Part(String text) {}
    private record GenerationConfig(List<String> responseModalities) {}
    private record RealtimeInputConfig(AutomaticActivityDetection automaticActivityDetection) {}
    private record AutomaticActivityDetection(boolean disabled, long prefixPaddingMs, long silenceDurationMs) {}
    private record AudioTranscriptionConfig() {}
}
