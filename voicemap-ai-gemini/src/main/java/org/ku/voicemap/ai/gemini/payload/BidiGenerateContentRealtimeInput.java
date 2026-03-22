package org.ku.voicemap.ai.gemini.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

public record BidiGenerateContentRealtimeInput(
    RealtimeInput realtimeInput
) {

    public static BidiGenerateContentRealtimeInput forAudio(String base64Audio) {
        return new BidiGenerateContentRealtimeInput(new RealtimeInput(new GeminiBlob(base64Audio, "audio/pcm"), null));
    }

    public static BidiGenerateContentRealtimeInput forText(String text) {
        return new BidiGenerateContentRealtimeInput(new RealtimeInput(null, text));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RealtimeInput(
        GeminiBlob audio,
        String text
    ) {
    }

    public record GeminiBlob(
        String data,
        String mimeType
    ) {
    }
}
