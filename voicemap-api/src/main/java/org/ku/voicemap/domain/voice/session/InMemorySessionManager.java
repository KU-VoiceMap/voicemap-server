package org.ku.voicemap.domain.voice.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

// TODO: Config로 관리 ?
// Sticky Session 필요
@Slf4j
@Component
public class InMemorySessionManager implements SessionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    @Override
    public void bindClient(String sessionId, WebSocketSession clientSession) {
        sessions.put(sessionId, clientSession);
    }

    @Override
    public void bindAgent(String sessionId, WebSocketSession agentSession) {
        agentSessions.put(sessionId, agentSession);
    }

    @Override
    public Optional<WebSocketSession> getClientSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<WebSocketSession> getAgentSession(String sessionId) {
        return Optional.ofNullable(agentSessions.get(sessionId));
    }

    @Override
    public void removeSession(String sessionId) {
        getClientSession(sessionId).ifPresent(session -> {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed to close client session: {}", sessionId, e);
            } finally {
                sessions.remove(sessionId);
            }
        });
        getAgentSession(sessionId).ifPresent(session -> {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Failed to close agent session: {}", sessionId, e);
            } finally {
                agentSessions.remove(sessionId);
            }
        });
    }
}
