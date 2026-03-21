package org.ku.voicemap.ai.gemini.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiRealtimeResponse(
    SetupComplete setupComplete,
    ServerContent serverContent,
    SessionResumptionUpdate sessionResumptionUpdate
) {

    public boolean isSetupComplete() {
        return setupComplete != null;
    }

    public boolean hasServerContent() {
        return serverContent != null;
    }

    public boolean hasSessionResumptionUpdate() {
        return sessionResumptionUpdate != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SetupComplete() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerContent(
        Transcription inputTranscription,
        Transcription outputTranscription,
        Boolean interrupted,
        ModelTurn modelTurn,
        Boolean turnComplete
    ) {

        public boolean isInterrupted() {
            return interrupted != null && interrupted;
        }

        public boolean isTurnComplete() {
            return turnComplete != null && turnComplete;
        }

        public boolean hasInputTranscription() {
            return inputTranscription != null;
        }

        public boolean hasOutputTranscription() {
            return outputTranscription != null;
        }

        public boolean hasModelTurn() {
            return modelTurn != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transcription(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelTurn(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(InlineData inlineData) {

        public boolean hasAudio() {
            return inlineData != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InlineData(String data, String mimeType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionResumptionUpdate(boolean resumable, String newHandle) {

        public boolean canResume() {
            return resumable && newHandle != null;
        }
    }
}
