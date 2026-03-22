package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.ku.voicemap.ai.gemini.payload.BidiGenerateContentRealtimeInput;
import org.ku.voicemap.ai.gemini.payload.SetupMessage;
import org.ku.voicemap.ai.gemini.realtime.GeminiSessionRegistry.SessionState;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.ku.voicemap.ai.realtime.AiRealtimeClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiRealtimeClient implements AiRealtimeClient {

    private final WebSocketClient webSocketClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final GeminiSessionRegistry sessionRegistry;

    @Override
    public void connect(String sessionId, String systemInstruction, AiAgentListener listener) {
        sessionRegistry.save(sessionId, new SessionState(null, systemInstruction, listener, null));
        doConnect(sessionId);
    }

    @Override
    public void sendAudio(String sessionId, String base64Audio) {
        sendRealtimeInput(sessionId, BidiGenerateContentRealtimeInput.forAudio(base64Audio));
    }

    @Override
    public void sendText(String sessionId, String text) {
        sendRealtimeInput(sessionId, BidiGenerateContentRealtimeInput.forText(text));
    }

    private void sendRealtimeInput(String sessionId, BidiGenerateContentRealtimeInput input) {
        SessionState state = sessionRegistry.get(sessionId);
        if (state == null || state.connection() == null) {
            throw new IllegalArgumentException("No active session: " + sessionId);
        }
        try {
            String payload = objectMapper.writeValueAsString(input);
            state.connection().sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.error("[GeminiRealtimeClient] Failed to send message for session: {}", sessionId, e);
        }
    }

    @Override
    public void disconnect(String sessionId) {
        SessionState state = sessionRegistry.remove(sessionId);
        if (state != null && state.connection() != null) {
            try {
                state.connection().close();
            } catch (Exception e) {
                log.error("[GeminiRealtimeClient] Failed to close connection for session: {}", sessionId, e);
            }
        }
    }

    private void doConnect(String sessionId) {
        SessionState state = sessionRegistry.get(sessionId);
        if (state == null) {
            return;
        }

        Runnable onDisconnect = () -> reconnect(sessionId);
        java.util.function.Consumer<String> onResumptionHandle = handle ->
            sessionRegistry.updateResumptionHandle(sessionId, handle);

        GeminiWebSocketHandler handler = new GeminiWebSocketHandler(
            sessionId, objectMapper, state.listener(), onDisconnect, onResumptionHandle
        );

        URI uri = UriComponentsBuilder.fromUriString(geminiProperties.urls().agentWebSocketUrl())
            .queryParam("key", geminiProperties.apiKey())
            .build()
            .toUri();

        webSocketClient.execute(handler, null, uri)
            .thenAccept(connection -> {
                sessionRegistry.updateConnection(sessionId, connection);
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
                sessionRegistry.remove(sessionId);
                return null;
            });
    }

    private void reconnect(String sessionId) {
        SessionState state = sessionRegistry.get(sessionId);
        if (state == null || state.resumptionHandle() == null) {
            log.info("[GeminiRealtimeClient] No resumption handle for session: {}, skipping reconnect", sessionId);
            return;
        }
        log.info("[GeminiRealtimeClient] Reconnecting session: {}", sessionId);
        doConnect(sessionId);
    }
}
