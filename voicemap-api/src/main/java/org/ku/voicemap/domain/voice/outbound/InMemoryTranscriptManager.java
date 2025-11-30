package org.ku.voicemap.domain.voice.outbound;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// TODO: Sticky session
@Component
public class InMemoryTranscriptManager implements TranscriptManager {

    private final Map<String, Map<ConversationRole, RoleTranscript>> transcripts = new ConcurrentHashMap<>();

    @Override
    public void appendTranscript(String sessionId, ConversationRole role, String text) {
        transcripts
            .computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>())
            .computeIfAbsent(role, r -> new RoleTranscript())
            .appendText(text);
    }

    @Override
    public String getTranscript(String sessionId, ConversationRole role) {
        return Optional.ofNullable(transcripts.get(sessionId))
            .map(roleTranscripts -> roleTranscripts.get(role))
            .map(RoleTranscript::getTranscript)
            .orElse("");
    }

    @Override
    public void clearTranscript(String sessionId) {
        transcripts.remove(sessionId);
    }

    private static class RoleTranscript {
        private final StringBuilder transcript = new StringBuilder();

        public synchronized void appendText(String text) {
            transcript.append(text);
        }

        public synchronized String getTranscript() {
            return transcript.toString();
        }
    }
}
