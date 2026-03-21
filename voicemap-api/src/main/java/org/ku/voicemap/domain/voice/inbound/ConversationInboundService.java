package org.ku.voicemap.domain.voice.inbound;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.entity.ChatContext;
import org.ku.voicemap.domain.chat.reposiotry.ChatContextRepository;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.chat.service.IdeaContextSummarizer;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationInboundService {

    private static final String DEFAULT_CHAT_TITLE = "새 대화";

    private final TokenProvider tokenProvider;
    private final ChatRepository chatRepository;
    private final ChatContextRepository chatContextRepository;
    private final ScriptRepository scriptRepository;
    private final VoiceSessionRepository sessionRepository;
    private final AgentConnector agentConnector;
    private final IdeaContextSummarizer ideaContextSummarizer;

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
            updateIdeaContextAsync(session.getChatId());
            sessionRepository.remove(session.getSessionId());
        });
    }

    private void updateIdeaContextAsync(String chatId) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Script> scripts = scriptRepository.findAllByChatIdOrderByCreatedAt(chatId);
                if (scripts.isEmpty()) {
                    return;
                }

                String previousContext = chatContextRepository.findFirstByChatIdOrderByCreatedAtDesc(chatId)
                    .map(ChatContext::getContext)
                    .orElse(null);

                String conversation = scripts.stream()
                    .map(s -> "[사용자]: " + s.getQuestion()
                        + (s.getAnswer() != null ? "\n[AI]: " + s.getAnswer() : ""))
                    .collect(Collectors.joining("\n\n"));

                String updatedContext = ideaContextSummarizer.summarize(previousContext, conversation);
                chatContextRepository.save(new ChatContext(chatId, updatedContext));

                log.info("[IdeaContext] Updated for chatId={}", chatId);
            } catch (Exception e) {
                log.error("[IdeaContext] Failed to update context for chatId={}", chatId, e);
            }
        });
    }
}
