package org.ku.voicemap.domain.voice.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.voice.outbound.ConversationOutboundService;
import org.ku.voicemap.domain.voice.outbound.ConversationRole;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
@RequiredArgsConstructor
public class GeminiAgentWebSocketHandler extends BinaryWebSocketHandler {

    private final String sessionId;
    private final ObjectMapper objectMapper;
    private final ConversationOutboundService outbound;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[GeminiAgentWebSocketHandler] Connection closed for session: {}, status: {}", sessionId, status);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String payload = StandardCharsets.UTF_8.decode(message.getPayload()).toString();
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root.has("setupComplete")) {
                outbound.sendSessionReady(sessionId);
                return;
            }

            if (root.has("serverContent")) {
                handleServerContent(root.get("serverContent"));
            }

        } catch (Exception e) {
            log.error("[GeminiAgentWebSocketHandler] Error processing message", e);
        }
    }

    private void handleServerContent(JsonNode content) {
        if (content.has("inputTranscription")) {
            String text = content.get("inputTranscription").path("text").asText();
            outbound.sendTranscript(sessionId, ConversationRole.USER, text);
        }

        if (content.has("outputTranscription")) {
            String text = content.get("outputTranscription").path("text").asText();
            outbound.sendTranscript(sessionId, ConversationRole.AGENT, text);
        }

        if (content.has("interrupted") && content.get("interrupted").asBoolean()) {
            outbound.sendInterrupted(sessionId);
        }

        if (content.has("modelTurn")) {
            JsonNode parts = content.get("modelTurn").get("parts");
            if (parts == null || !parts.isArray()) {
                return;
            }
            parts.valueStream()
                .filter(part -> part.has("inlineData"))
                .map(part -> part.get("inlineData").path("data").asText())
                .findFirst()
                .ifPresent(base64Audio -> outbound.sendAudioOutput(sessionId, base64Audio));
        }

        if (content.has("turnComplete") && content.get("turnComplete").asBoolean()) {
            outbound.sendTurnComplete(sessionId);
        }
    }
}
