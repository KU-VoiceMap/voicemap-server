package org.ku.voicemap.domain.voice.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.outbound.payload.AudioOutputPayload;
import org.ku.voicemap.domain.voice.outbound.payload.ServerPayload;
import org.ku.voicemap.domain.voice.outbound.payload.SessionReadyPayload;
import org.ku.voicemap.domain.voice.outbound.payload.TranscriptPayload;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOutboundService {

    private final VoiceSessionRepository sessionRepository;
    private final ScriptRepository scriptRepository;
    private final ObjectMapper objectMapper;

    public void sendSessionReady(String sessionId) {
        send(sessionId, ServerMessageType.SESSION_READY, new SessionReadyPayload(sessionId));
    }

    public void sendAudioOutput(String sessionId, String base64Audio) {
        send(sessionId, ServerMessageType.AUDIO_OUTPUT, new AudioOutputPayload(base64Audio));
    }

    public void sendTranscript(String sessionId, ConversationRole role, String text) {
        VoiceSession session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        session.appendTranscript(role, text);
        send(sessionId, ServerMessageType.TRANSCRIPT, new TranscriptPayload(role.name(), text));
    }

    public void sendInterrupted(String sessionId) {
        send(sessionId, ServerMessageType.INTERRUPTED, Collections.emptyMap());
    }

    public void sendTurnComplete(String sessionId) {
        VoiceSession session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        String userTranscript = session.getTranscript(ConversationRole.USER);
        String agentTranscript = session.getTranscript(ConversationRole.AGENT);

        log.info("[USER]: {}", userTranscript);
        log.info("[AGENT]: {}", agentTranscript);

        saveScript(session.getChatId(), userTranscript, agentTranscript);
        session.clearTranscript();

        send(sessionId, ServerMessageType.TURN_COMPLETED, Collections.emptyMap());
    }

    private void saveScript(String chatId, String question, String answer) {
        if (question.isBlank()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Script script = new Script(chatId, question, now);
        if (!answer.isBlank()) {
            script.answer(answer, now);
        }
        scriptRepository.save(script);
    }

    private void send(String sessionId, ServerMessageType type, Object payload) {
        VoiceSession session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        WebSocketSession clientConnection = session.getClientConnection();
        if (!clientConnection.isOpen()) {
            sessionRepository.remove(sessionId);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(new ServerPayload(type.name(), payload));
            clientConnection.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("[ConversationOutboundService] Failed to send message: {}", type, e);
        }
    }
}
