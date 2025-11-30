package org.ku.voicemap.domain.voice.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.voice.outbound.payload.AudioOutputPayload;
import org.ku.voicemap.domain.voice.outbound.payload.ServerPayload;
import org.ku.voicemap.domain.voice.outbound.payload.SessionReadyPayload;
import org.ku.voicemap.domain.voice.outbound.payload.TranscriptPayload;
import org.ku.voicemap.domain.voice.session.SessionManager;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOutboundService {

    private final ChatRepository chatRepository;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final TranscriptManager transcriptManager;

    public void sendSessionReady(String sessionId) {
        send(sessionId, ServerMessageType.SESSION_READY, new SessionReadyPayload(sessionId));
    }

    public void sendAudioOutput(String sessionId, String base64Audio) {
        send(sessionId, ServerMessageType.AUDIO_OUTPUT, new AudioOutputPayload(base64Audio));
    }

    public void sendTranscript(String sessionId, ConversationRole role, String text) {
        transcriptManager.appendTranscript(sessionId, role, text);
        send(sessionId, ServerMessageType.TRANSCRIPT, new TranscriptPayload(role.name(), text));
    }

    public void sendInterrupted(String sessionId) {
        send(sessionId, ServerMessageType.INTERRUPTED, Collections.emptyMap());
    }

    public void sendTurnComplete(String sessionId) {
        String userTranscript = transcriptManager.getTranscript(sessionId, ConversationRole.USER);
        String agentTranscript = transcriptManager.getTranscript(sessionId, ConversationRole.AGENT);
        log.info("[USER]: {}", userTranscript);
        log.info("[AGENT]: {}", agentTranscript);
        transcriptManager.clearTranscript(sessionId);
        // TODO: Script 저장
        send(sessionId, ServerMessageType.TURN_COMPLETED, Collections.emptyMap());
    }

    private void send(String sessionId, ServerMessageType type, Object payload) {
        WebSocketSession clientSession = sessionManager.getClientSession(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!clientSession.isOpen()) {
            sessionManager.removeSession(sessionId);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(new ServerPayload(type.name(), payload));
            clientSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("[ConversationOutboundService] Failed to send message: {}", type, e);
        }
    }
}
