package org.ku.voicemap.domain.voice.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConversationInboundService inbound;

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
                inbound.handleAudioInput(payload);
            }
        }
    }
}
