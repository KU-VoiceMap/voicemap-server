package org.ku.voicemap.domain.voice.inbound;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.session.SessionManager;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
public class ConversationInboundService {

    private final ChatRepository chatRepository;
    private final SessionManager sessionManager;
    private final AgentConnector agentConnector;

    public void initializeSession(WebSocketSession clientSession, SessionInitPayload payload) {
        // TODO: 인증 추가
        String sessionId = UUID.randomUUID().toString();
        sessionManager.bindClient(sessionId, clientSession);
        agentConnector.connect(sessionId);
    }

    public void handleAudioInput(WebSocketSession clientSession, AudioInputPayload payload) {
        WebSocketSession session = sessionManager.getClientSession(payload.sessionId())
            .orElseThrow(() -> new IllegalStateException("Session not found: " + payload.sessionId()));
        if (!session.getId().equals(clientSession.getId())) {
            throw new IllegalStateException("Session ID mismatch");
        }
        agentConnector.sendAudio(payload.sessionId(), payload.data());
    }

    public void disconnectSession(WebSocketSession clientSession, String sessionId) {
        WebSocketSession session = sessionManager.getClientSession(sessionId)
            .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionId));
        if (!session.getId().equals(clientSession.getId())) {
            throw new IllegalStateException("Session ID mismatch");
        }
        agentConnector.disconnect(sessionId);
        sessionManager.removeSession(sessionId);
    }
}
