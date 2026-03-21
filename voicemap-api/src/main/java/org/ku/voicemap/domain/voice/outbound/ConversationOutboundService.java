package org.ku.voicemap.domain.voice.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.ku.voicemap.ai.realtime.AiRole;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.outbound.payload.AudioOutputPayload;
import org.ku.voicemap.domain.voice.outbound.payload.ServerPayload;
import org.ku.voicemap.domain.voice.outbound.payload.SessionReadyPayload;
import org.ku.voicemap.domain.voice.outbound.payload.TitleUpdatedPayload;
import org.ku.voicemap.domain.voice.outbound.payload.TranscriptPayload;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOutboundService implements AiAgentListener {

    private final VoiceSessionRepository sessionRepository;
    private final ScriptRepository scriptRepository;
    private final ChatRepository chatRepository;
    private final AiChatClient aiChatClient;
    private final AiInstructionProperties aiInstructionProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onSessionReady(String sessionId) {
        sendSessionReady(sessionId);
    }

    @Override
    public void onTranscript(String sessionId, AiRole role, String text) {
        ConversationRole conversationRole = switch (role) {
            case USER -> ConversationRole.USER;
            case AGENT -> ConversationRole.AGENT;
        };
        sendTranscript(sessionId, conversationRole, text);
    }

    @Override
    public void onAudioOutput(String sessionId, String base64Audio) {
        sendAudioOutput(sessionId, base64Audio);
    }

    @Override
    public void onInterrupted(String sessionId) {
        sendInterrupted(sessionId);
    }

    @Override
    public void onTurnComplete(String sessionId) {
        sendTurnComplete(sessionId);
    }

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

        boolean isFirstTurn = scriptRepository.findAllByChatIdOrderByCreatedAt(session.getChatId()).isEmpty();

        saveScript(session.getChatId(), userTranscript, agentTranscript);
        session.clearTranscript();

        send(sessionId, ServerMessageType.TURN_COMPLETED, Collections.emptyMap());

        if (isFirstTurn && !userTranscript.isBlank()) {
            generateAndUpdateTitle(sessionId, session.getChatId(), userTranscript, agentTranscript);
        }
    }

    private void generateAndUpdateTitle(String sessionId, String chatId, String question, String answer) {
        try {
            String content = "[사용자]: " + question + "\n[AI]: " + answer;
            String systemInstruction = aiInstructionProperties.instructions().chat();
            String title = aiChatClient.generateTitle(systemInstruction, content).title();

            Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));
            chat.updateTitle(title);
            chatRepository.save(chat);

            send(sessionId, ServerMessageType.TITLE_UPDATED, new TitleUpdatedPayload(chatId, title));
            log.info("[Title Generated] chatId={}, title={}", chatId, title);
        } catch (Exception e) {
            log.error("[ConversationOutboundService] Failed to generate title for chatId={}", chatId, e);
        }
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

        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.updateLastInteractedAt(now);
            chatRepository.save(chat);
        });
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
