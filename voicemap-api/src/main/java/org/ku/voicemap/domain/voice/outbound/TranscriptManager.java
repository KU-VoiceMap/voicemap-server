package org.ku.voicemap.domain.voice.outbound;

public interface TranscriptManager {

    void appendTranscript(String sessionId, ConversationRole role, String text);

    String getTranscript(String sessionId, ConversationRole role);

    void clearTranscript(String sessionId);
}
