package org.ku.voicemap.ai.gemini.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiChatResponse(List<Candidate> candidates) {

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates in Gemini response");
        }
        Candidate candidate = candidates.getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException("No content parts in Gemini response");
        }
        return candidate.content().parts().getFirst().text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}
}
