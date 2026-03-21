package org.ku.voicemap.domain.voice.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.ku.voicemap.domain.voice.outbound.ConversationRole;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class VoiceSession {

    private final String sessionId;
    private final String memberNumber;
    private final String chatId;
    private final WebSocketSession clientConnection;
    private volatile WebSocketSession agentConnection;
    private volatile String resumptionHandle;
    private final Map<ConversationRole, StringBuilder> transcripts = new ConcurrentHashMap<>();

    public VoiceSession(String memberNumber, String chatId, WebSocketSession clientConnection) {
        this.sessionId = UUID.randomUUID().toString();
        this.memberNumber = memberNumber;
        this.chatId = chatId;
        this.clientConnection = clientConnection;
    }

    public void bindAgentConnection(WebSocketSession agentConnection) {
        this.agentConnection = agentConnection;
    }

    public void updateResumptionHandle(String handle) {
        this.resumptionHandle = handle;
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

