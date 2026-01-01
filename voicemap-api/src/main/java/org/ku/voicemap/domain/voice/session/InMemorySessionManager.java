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
public class InMemorySessionManager implements SessionManager {

    private final Map<String, VoiceSessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> clientSessionIdToSessionId = new ConcurrentHashMap<>();

    @Override
    public VoiceSessionContext createSession(String memberNumber, String chatId, WebSocketSession clientSession) {
        VoiceSessionContext context = new VoiceSessionContext(memberNumber, chatId, clientSession);
        sessions.put(context.getSessionId(), context);
        clientSessionIdToSessionId.put(clientSession.getId(), context.getSessionId());
        return context;
    }

    @Override
    public void bindAgent(String sessionId, WebSocketSession agentSession) {
        VoiceSessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.bindAgentSession(agentSession);
        }
    }

    @Override
    public Optional<VoiceSessionContext> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void removeSession(String sessionId) {
        VoiceSessionContext context = sessions.remove(sessionId);
        if (context == null) {
            return;
        }

        clientSessionIdToSessionId.remove(context.getClientSession().getId());

        try {
            context.getClientSession().close();
        } catch (Exception e) {
            log.error("Failed to close client session: {}", sessionId, e);
        }

        WebSocketSession agentSession = context.getAgentSession();
        if (agentSession != null) {
            try {
                agentSession.close();
            } catch (Exception e) {
                log.error("Failed to close agent session: {}", sessionId, e);
            }
        }
    }
}
