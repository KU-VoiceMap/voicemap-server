package org.ku.voicemap.domain.voice.inbound;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.session.SessionManager;
import org.ku.voicemap.domain.voice.session.VoiceSessionContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
public class ConversationInboundService {

    private static final String DEFAULT_CHAT_TITLE = "새 대화";

    private final TokenProvider tokenProvider;
    private final ChatRepository chatRepository;
    private final SessionManager sessionManager;
    private final AgentConnector agentConnector;

    public void initializeSession(WebSocketSession clientSession, SessionInitPayload payload) {
        String memberNumber = tokenProvider.extractMemberNumber(payload.token());

        String chatId = Optional.ofNullable(payload.chatId())
            .map(id -> chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with id: " + id))
                .getId())
            .orElseGet(() -> chatRepository.save(new Chat(memberNumber, DEFAULT_CHAT_TITLE)).getId());

        VoiceSessionContext context = sessionManager.createSession(memberNumber, chatId, clientSession);
        agentConnector.connect(context.getSessionId());
    }

    public void handleAudioInput(AudioInputPayload payload) {
        agentConnector.sendAudio(payload.sessionId(), payload.data());
    }

    public void disconnectSession(String sessionId) {
        agentConnector.disconnect(sessionId);
        sessionManager.removeSession(sessionId);
    }
}
