package org.ku.voicemap.domain.voice.inbound;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.IdeaContextResult;
import org.ku.voicemap.ai.realtime.AiRealtimeClient;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.entity.ChatContext;
import org.ku.voicemap.domain.chat.repository.ChatContextRepository;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.inbound.payload.TextInputPayload;
import org.ku.voicemap.domain.voice.outbound.ConversationOutboundService;
import org.ku.voicemap.domain.voice.outbound.ConversationRole;
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
    private final AiRealtimeClient aiRealtimeClient;
    private final AiChatClient aiChatClient;
    private final AiInstructionProperties aiInstructionProperties;
    private final ConversationOutboundService outbound;

    public void initializeSession(WebSocketSession clientConnection, SessionInitPayload payload) {
        String memberNumber = tokenProvider.extractMemberNumber(payload.token());

        String chatId = Optional.ofNullable(payload.chatId())
            .map(id -> chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with id: " + id))
                .getId())
            .orElseGet(() -> chatRepository.save(new Chat(memberNumber, DEFAULT_CHAT_TITLE)).getId());

        VoiceSession session = new VoiceSession(memberNumber, chatId, clientConnection);
        sessionRepository.save(session);

        String systemInstruction = buildSystemInstruction(chatId);
        aiRealtimeClient.connect(session.getSessionId(), systemInstruction, outbound);
    }

    public void handleAudioInput(WebSocketSession clientConnection, AudioInputPayload payload) {
        VoiceSession session = sessionRepository.findByClientConnection(clientConnection)
            .orElseThrow(() -> new IllegalStateException("Session not initialized"));
        aiRealtimeClient.sendAudio(session.getSessionId(), payload.data());
    }

    public void handleTextInput(WebSocketSession clientConnection, TextInputPayload payload) {
        VoiceSession session = sessionRepository.findByClientConnection(clientConnection)
            .orElseThrow(() -> new IllegalStateException("Session not initialized"));
        session.appendTranscript(ConversationRole.USER, payload.text());
        aiRealtimeClient.sendText(session.getSessionId(), payload.text());
    }

    public void disconnectSession(WebSocketSession clientConnection) {
        sessionRepository.findByClientConnection(clientConnection).ifPresent(session -> {
            aiRealtimeClient.disconnect(session.getSessionId());
            updateIdeaContextAsync(session.getChatId());
            sessionRepository.remove(session.getSessionId());
        });
    }

    private String buildSystemInstruction(String chatId) {
        String baseInstruction = aiInstructionProperties.instructions().agent();
        String chatContext = chatContextRepository.findFirstByChatIdOrderByCreatedAtDesc(chatId)
            .map(ChatContext::getContext)
            .orElse(null);

        if (chatContext == null || chatContext.isBlank()) {
            return baseInstruction;
        }

        return baseInstruction + "\n\n---\n"
            + "## 이전 대화 맥락\n"
            + "이 사용자와 이전에 아이디어 빌딩을 진행한 기록입니다.\n"
            + "자연스럽게 이어서 대화하세요. 이전에 내려진 결정은 존중하되, "
            + "사용자가 방향을 바꾸고 싶어하면 유연하게 수용하세요.\n\n"
            + chatContext + "\n---";
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

                String instruction = aiInstructionProperties.instructions().contextSummarizer();

                StringBuilder prompt = new StringBuilder();
                if (previousContext != null && !previousContext.isBlank()) {
                    prompt.append("## 이전 아이디어 진행 상태\n").append(previousContext).append("\n\n");
                }
                prompt.append("## 새로운 대화 내용\n").append(conversation);
                prompt.append("\n\n위 내용을 바탕으로 아이디어 진행 상태를 정리하세요.");

                IdeaContextResult result = aiChatClient.summarizeContext(instruction, prompt.toString());
                String updatedContext = formatIdeaContext(result);
                chatContextRepository.save(new ChatContext(chatId, updatedContext));

                log.info("[IdeaContext] Updated for chatId={}", chatId);
            } catch (Exception e) {
                log.error("[IdeaContext] Failed to update context for chatId={}", chatId, e);
            }
        });
    }

    private String formatIdeaContext(IdeaContextResult result) {
        return "### 핵심 아이디어\n" + result.coreIdea()
            + "\n\n### 내려진 결정들\n" + result.decisions()
            + "\n\n### 현재 단계\n" + result.currentPhase()
            + "\n\n### 아직 탐색되지 않은 영역\n" + result.unexplored();
    }
}
