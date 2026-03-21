package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.ku.voicemap.ai.realtime.AiRole;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
@RequiredArgsConstructor
public class GeminiWebSocketHandler extends BinaryWebSocketHandler {

    private final String sessionId;
    private final ObjectMapper objectMapper;
    private final AiAgentListener listener;
    private final Runnable onDisconnect;
    private final Consumer<String> onResumptionHandle;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[GeminiWebSocketHandler] Connection closed for session: {}, status: {}", sessionId, status);
        onDisconnect.run();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String payload = StandardCharsets.UTF_8.decode(message.getPayload()).toString();
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root.has("setupComplete")) {
                listener.onSessionReady(sessionId);
                return;
            }

            if (root.has("serverContent")) {
                handleServerContent(root.get("serverContent"));
            }

            if (root.has("sessionResumptionUpdate")) {
                handleSessionResumptionUpdate(root.get("sessionResumptionUpdate"));
            }
        } catch (Exception e) {
            log.error("[GeminiWebSocketHandler] Error processing message", e);
        }
    }

    private void handleServerContent(JsonNode content) {
        if (content.has("inputTranscription")) {
            String text = content.get("inputTranscription").path("text").asText();
            listener.onTranscript(sessionId, AiRole.USER, text);
        }

        if (content.has("outputTranscription")) {
            String text = content.get("outputTranscription").path("text").asText();
            listener.onTranscript(sessionId, AiRole.AGENT, text);
        }

        if (content.has("interrupted") && content.get("interrupted").asBoolean()) {
            listener.onInterrupted(sessionId);
        }

        if (content.has("modelTurn")) {
            JsonNode parts = content.get("modelTurn").get("parts");
            if (parts != null && parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("inlineData")) {
                        String base64Audio = part.get("inlineData").path("data").asText();
                        listener.onAudioOutput(sessionId, base64Audio);
                    }
                }
            }
        }

        if (content.has("turnComplete") && content.get("turnComplete").asBoolean()) {
            listener.onTurnComplete(sessionId);
        }
    }

    private void handleSessionResumptionUpdate(JsonNode update) {
        boolean resumable = update.path("resumable").asBoolean(false);
        String newHandle = update.path("newHandle").asText(null);
        if (resumable && newHandle != null) {
            onResumptionHandle.accept(newHandle);
        }
    }
}
