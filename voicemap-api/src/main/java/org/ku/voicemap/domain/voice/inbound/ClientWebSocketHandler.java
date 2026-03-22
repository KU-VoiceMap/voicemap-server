package org.ku.voicemap.domain.voice.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.inbound.payload.TextInputPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConversationInboundService inbound;

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[ClientWebSocketHandler] Connection closed, status: {}", status);
        inbound.disconnectSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws JsonProcessingException {
        // TODO: Add error handling
        JsonNode root = objectMapper.readTree(message.getPayload());
        JsonNode messagePayload = root.get("payload");
        ClientMessageType messageType = ClientMessageType.valueOf(root.get("type").asText());

        switch (messageType) {
            case SESSION_INIT -> {
                SessionInitPayload payload = objectMapper.treeToValue(messagePayload, SessionInitPayload.class);
                inbound.initializeSession(session, payload);
            }
            case AUDIO_INPUT -> {
                AudioInputPayload payload = objectMapper.treeToValue(messagePayload, AudioInputPayload.class);
                inbound.handleAudioInput(session, payload);
            }
            case TEXT_INPUT -> {
                TextInputPayload payload = objectMapper.treeToValue(messagePayload, TextInputPayload.class);
                inbound.handleTextInput(session, payload);
            }
        }
    }
}
