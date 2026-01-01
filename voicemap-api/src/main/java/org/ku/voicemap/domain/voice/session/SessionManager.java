package org.ku.voicemap.domain.voice.session;

import java.util.Optional;
import org.springframework.web.socket.WebSocketSession;

public interface SessionManager {

    VoiceSessionContext createSession(String memberNumber, String chatId, WebSocketSession clientSession);

    void bindAgent(String sessionId, WebSocketSession agentSession);

    Optional<VoiceSessionContext> getSession(String sessionId);

    void removeSession(String sessionId);
}
