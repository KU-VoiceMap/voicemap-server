package org.ku.voicemap.domain.voice.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.ai.gemini.payload.AiProperties;
import org.ku.voicemap.domain.voice.ai.gemini.payload.BidiGenerateContentRealtimeInput;
import org.ku.voicemap.domain.voice.ai.gemini.payload.SetupMessage;
import org.ku.voicemap.domain.voice.outbound.ConversationOutboundService;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AiProperties.class)
public class GeminiAgentConnector implements AgentConnector {

    private static final String GEMINI_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent";

    private final WebSocketClient webSocketClient;
    private final VoiceSessionRepository sessionRepository;

    private final AiProperties aiProperties;

    // Handler 생성 시에만 사용한다.
    private final ObjectMapper objectMapper;
    private final ConversationOutboundService outbound;

    @Override
    public void connect(String sessionId) {
        webSocketClient.execute(
                new GeminiAgentWebSocketHandler(sessionId, objectMapper, outbound),
                GEMINI_URL + "?key=" + aiProperties.apiKey()
            ).thenAccept(agentConnection ->
                sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                    session.bindAgentConnection(agentConnection);
                    try {
                        SetupMessage setupMessage = SetupMessage.create(aiProperties.systemInstruction());
                        agentConnection.sendMessage(new TextMessage(objectMapper.writeValueAsString(setupMessage)));
                    } catch (IOException e) {
                        log.error("[GeminiAgentConnector] Failed to send setup message for session: {}", sessionId, e);
                    }
                }))
            .exceptionally(e -> {
                log.error("[GeminiAgentConnector] Failed to connect to Gemini for session: {}", sessionId, e);
                sessionRepository.remove(sessionId);
                return null;
            });
    }

    @Override
    public void sendAudio(String sessionId, String base64Audio) {
        WebSocketSession agentConnection = sessionRepository.findBySessionId(sessionId)
            .map(VoiceSession::getAgentConnection)
            .orElseThrow(() -> new IllegalArgumentException("Agent connection not found: " + sessionId));
        try {
            BidiGenerateContentRealtimeInput input = new BidiGenerateContentRealtimeInput(base64Audio);
            String payload = objectMapper.writeValueAsString(input);
            agentConnection.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.error("[GeminiAgentConnector] Failed to send audio, sessionId: {}", sessionId, e);
        }
    }

    @Override
    public void disconnect(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
            .map(VoiceSession::getAgentConnection)
            .ifPresent(agentConnection -> {
                try {
                    agentConnection.close();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to close agent connection: " + sessionId, e);
                }
            });
    }
}
