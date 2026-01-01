package org.ku.voicemap.domain.voice.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

// TODO: Redis 기반 구현으로 교체 시 Sticky Session 필요
@Slf4j
@Component
public class InMemoryVoiceSessionRepository implements VoiceSessionRepository {

    private static final String SESSION_ID_ATTRIBUTE = "sessionId";

    private final Map<String, VoiceSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(VoiceSession session) {
        sessions.put(session.getSessionId(), session);
        session.getClientConnection().getAttributes().put(SESSION_ID_ATTRIBUTE, session.getSessionId());
    }

    @Override
    public Optional<VoiceSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<VoiceSession> findByClientConnection(WebSocketSession clientConnection) {
        String sessionId = (String) clientConnection.getAttributes().get(SESSION_ID_ATTRIBUTE);
        if (sessionId == null) {
            return Optional.empty();
        }
        return findBySessionId(sessionId);
    }

    @Override
    public void remove(String sessionId) {
        VoiceSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }

        try {
            session.getClientConnection().close();
        } catch (Exception e) {
            log.error("Failed to close client connection: {}", sessionId, e);
        }

        WebSocketSession agentConnection = session.getAgentConnection();
        if (agentConnection != null) {
            try {
                agentConnection.close();
            } catch (Exception e) {
                log.error("Failed to close agent connection: {}", sessionId, e);
            }
        }
    }
}

