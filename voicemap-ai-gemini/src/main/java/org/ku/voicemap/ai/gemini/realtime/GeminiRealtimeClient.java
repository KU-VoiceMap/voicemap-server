package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.ku.voicemap.ai.gemini.payload.BidiGenerateContentRealtimeInput;
import org.ku.voicemap.ai.gemini.payload.SetupMessage;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.ku.voicemap.ai.realtime.AiRealtimeClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@Component
public class GeminiRealtimeClient implements AiRealtimeClient {

    private final WebSocketClient webSocketClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    public GeminiRealtimeClient(WebSocketClient webSocketClient,
                                GeminiProperties geminiProperties,
                                ObjectMapper objectMapper) {
        this.webSocketClient = webSocketClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    GeminiRealtimeClient(WebSocketClient webSocketClient,
                         GeminiProperties geminiProperties,
                         ObjectMapper objectMapper,
                         ConcurrentHashMap<String, SessionState> sessions) {
        this.webSocketClient = webSocketClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.sessions.putAll(sessions);
    }

    @Override
    public void connect(String sessionId, String systemInstruction, AiAgentListener listener) {
        sessions.put(sessionId, new SessionState(null, systemInstruction, listener, null));
        doConnect(sessionId);
    }

    @Override
    public void sendAudio(String sessionId, String base64Audio) {
        SessionState state = sessions.get(sessionId);
        if (state == null || state.connection() == null) {
            throw new IllegalArgumentException("No active session: " + sessionId);
        }
        try {
            BidiGenerateContentRealtimeInput input = new BidiGenerateContentRealtimeInput(base64Audio);
            String payload = objectMapper.writeValueAsString(input);
            state.connection().sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.error("[GeminiRealtimeClient] Failed to send audio for session: {}", sessionId, e);
        }
    }

    @Override
    public void disconnect(String sessionId) {
        SessionState state = sessions.remove(sessionId);
        if (state != null && state.connection() != null) {
            try {
                state.connection().close();
            } catch (Exception e) {
                log.error("[GeminiRealtimeClient] Failed to close connection for session: {}", sessionId, e);
            }
        }
    }

    private void doConnect(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return;
        }

        Runnable onDisconnect = () -> reconnect(sessionId);
        java.util.function.Consumer<String> onResumptionHandle = handle ->
            sessions.computeIfPresent(sessionId, (id, s) -> s.withResumptionHandle(handle));

        GeminiWebSocketHandler handler = new GeminiWebSocketHandler(
            sessionId, objectMapper, state.listener(), onDisconnect, onResumptionHandle
        );

        String url = geminiProperties.urls().agentWebSocketUrl() + "?key=" + geminiProperties.apiKey();

        webSocketClient.execute(handler, url)
            .thenAccept(connection -> {
                sessions.computeIfPresent(sessionId, (id, s) -> s.withConnection(connection));
                try {
                    String model = geminiProperties.models().agentModel();
                    SetupMessage setup = SetupMessage.create(model, state.systemInstruction(), state.resumptionHandle());
                    connection.sendMessage(new TextMessage(objectMapper.writeValueAsString(setup)));
                } catch (IOException e) {
                    log.error("[GeminiRealtimeClient] Failed to send setup for session: {}", sessionId, e);
                }
            })
            .exceptionally(e -> {
                log.error("[GeminiRealtimeClient] Failed to connect for session: {}", sessionId, e);
                sessions.remove(sessionId);
                return null;
            });
    }

    private void reconnect(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null || state.resumptionHandle() == null) {
            log.info("[GeminiRealtimeClient] No resumption handle for session: {}, skipping reconnect", sessionId);
            return;
        }
        log.info("[GeminiRealtimeClient] Reconnecting session: {}", sessionId);
        doConnect(sessionId);
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
