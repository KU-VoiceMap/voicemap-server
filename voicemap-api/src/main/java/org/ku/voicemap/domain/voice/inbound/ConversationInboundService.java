package org.ku.voicemap.domain.voice.inbound;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
public class ConversationInboundService {

    private static final String DEFAULT_CHAT_TITLE = "새 대화";

    private final TokenProvider tokenProvider;
    private final ChatRepository chatRepository;
    private final VoiceSessionRepository sessionRepository;
    private final AgentConnector agentConnector;

    public void initializeSession(WebSocketSession clientConnection, SessionInitPayload payload) {
        String memberNumber = tokenProvider.extractMemberNumber(payload.token());

        String chatId = Optional.ofNullable(payload.chatId())
            .map(id -> chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with id: " + id))
                .getId())
            .orElseGet(() -> chatRepository.save(new Chat(memberNumber, DEFAULT_CHAT_TITLE)).getId());

        VoiceSession session = new VoiceSession(memberNumber, chatId, clientConnection);
        sessionRepository.save(session);
        agentConnector.connect(session.getSessionId());
    }

    public void handleAudioInput(WebSocketSession clientConnection, AudioInputPayload payload) {
        VoiceSession session = sessionRepository.findByClientConnection(clientConnection)
            .orElseThrow(() -> new IllegalStateException("Session not initialized"));
        agentConnector.sendAudio(session.getSessionId(), payload.data());
    }

    public void disconnectSession(WebSocketSession clientConnection) {
        sessionRepository.findByClientConnection(clientConnection).ifPresent(session -> {
            agentConnector.disconnect(session.getSessionId());
            sessionRepository.remove(session.getSessionId());
        });
    }
}
