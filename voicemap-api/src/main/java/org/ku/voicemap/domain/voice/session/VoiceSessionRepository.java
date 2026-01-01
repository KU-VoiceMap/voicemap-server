package org.ku.voicemap.domain.voice.session;

import java.util.Optional;
import org.springframework.web.socket.WebSocketSession;

public interface VoiceSessionRepository {

    void save(VoiceSession session);

    Optional<VoiceSession> findBySessionId(String sessionId);

    Optional<VoiceSession> findByClientConnection(WebSocketSession clientConnection);

    void remove(String sessionId);
}

