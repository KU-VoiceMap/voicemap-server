package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.gemini.payload.GeminiRealtimeResponse;
import org.ku.voicemap.ai.gemini.payload.GeminiRealtimeResponse.ServerContent;
import org.ku.voicemap.ai.gemini.payload.GeminiRealtimeResponse.SessionResumptionUpdate;
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
            GeminiRealtimeResponse response = objectMapper.readValue(payload, GeminiRealtimeResponse.class);

            if (response.isSetupComplete()) {
                listener.onSessionReady(sessionId);
                return;
            }

            if (response.hasServerContent()) {
                handleServerContent(response.serverContent());
            }

            if (response.hasSessionResumptionUpdate()) {
                handleSessionResumptionUpdate(response.sessionResumptionUpdate());
            }
        } catch (Exception e) {
            log.error("[GeminiWebSocketHandler] Error processing message", e);
        }
    }

    private void handleServerContent(ServerContent content) {
        if (content.hasInputTranscription()) {
            listener.onTranscript(sessionId, AiRole.USER, content.inputTranscription().text());
        }

        if (content.hasOutputTranscription()) {
            listener.onTranscript(sessionId, AiRole.AGENT, content.outputTranscription().text());
        }

        if (content.isInterrupted()) {
            listener.onInterrupted(sessionId);
        }

        if (content.hasModelTurn()) {
            for (var part : content.modelTurn().parts()) {
                if (part.hasAudio()) {
                    listener.onAudioOutput(sessionId, part.inlineData().data());
                }
            }
        }

        if (content.isTurnComplete()) {
            listener.onTurnComplete(sessionId);
        }
    }

    private void handleSessionResumptionUpdate(SessionResumptionUpdate update) {
        if (update.canResume()) {
            onResumptionHandle.accept(update.newHandle());
        }
    }
}
