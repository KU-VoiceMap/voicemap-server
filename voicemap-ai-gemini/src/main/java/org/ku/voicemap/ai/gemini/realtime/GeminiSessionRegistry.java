package org.ku.voicemap.ai.gemini.realtime;

import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class GeminiSessionRegistry {

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    public void save(String sessionId, SessionState state) {
        sessions.put(sessionId, state);
    }

    public SessionState get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void updateConnection(String sessionId, WebSocketSession connection) {
        sessions.computeIfPresent(sessionId, (id, state) -> state.withConnection(connection));
    }

    public void updateResumptionHandle(String sessionId, String handle) {
        sessions.computeIfPresent(sessionId, (id, state) -> state.withResumptionHandle(handle));
    }

    public SessionState remove(String sessionId) {
        return sessions.remove(sessionId);
    }

    record SessionState(
        WebSocketSession connection,
        String systemInstruction,
        AiAgentListener listener,
        String resumptionHandle
    ) {
        SessionState withConnection(WebSocketSession conn) {
            return new SessionState(conn, systemInstruction, listener, resumptionHandle);
        }

        SessionState withResumptionHandle(String handle) {
            return new SessionState(connection, systemInstruction, listener, handle);
        }
    }
}
