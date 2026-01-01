package org.ku.voicemap.domain.voice.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.ku.voicemap.domain.voice.outbound.ConversationRole;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class VoiceSessionContext {

    private final String sessionId;
    private final String memberNumber;
    private final String chatId;
    private final WebSocketSession clientSession;
    private volatile WebSocketSession agentSession;
    private final Map<ConversationRole, StringBuilder> transcripts = new ConcurrentHashMap<>();

    public VoiceSessionContext(String memberNumber, String chatId, WebSocketSession clientSession) {
        this.sessionId = UUID.randomUUID().toString();
        this.memberNumber = memberNumber;
        this.chatId = chatId;
        this.clientSession = clientSession;
    }

    public void bindAgentSession(WebSocketSession agentSession) {
        this.agentSession = agentSession;
    }

    public synchronized void appendTranscript(ConversationRole role, String text) {
        transcripts.computeIfAbsent(role, r -> new StringBuilder()).append(text);
    }

    public String getTranscript(ConversationRole role) {
        StringBuilder transcript = transcripts.get(role);
        return transcript != null ? transcript.toString() : "";
    }

    public void clearTranscript() {
        transcripts.clear();
    }
}
