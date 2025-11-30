package org.ku.voicemap.domain.voice.session;

import java.util.Optional;
import org.springframework.web.socket.WebSocketSession;

public interface SessionManager {

    void bindClient(String sessionId, WebSocketSession clientSession);

    void bindAgent(String sessionId, WebSocketSession agentSession);

    Optional<WebSocketSession> getClientSession(String sessionId);

    Optional<WebSocketSession> getAgentSession(String sessionId);

    void removeSession(String sessionId);
}
