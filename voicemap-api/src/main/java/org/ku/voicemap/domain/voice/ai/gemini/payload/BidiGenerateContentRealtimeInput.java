package org.ku.voicemap.domain.voice.ai.gemini.payload;

public record BidiGenerateContentRealtimeInput(
    RealtimeInput realtimeInput
) {

    public BidiGenerateContentRealtimeInput(String base64Audio) {
        this(new RealtimeInput(base64Audio));
    }

    public record RealtimeInput(
        GeminiBlob audio
    ) {
        public RealtimeInput(String base64AudioData) {
            this(new GeminiBlob(base64AudioData, "audio/pcm"));
        }
    }

    public record GeminiBlob(
        String data,
        String mimeType
    ) {
    }
}
