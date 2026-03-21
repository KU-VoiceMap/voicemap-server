package org.ku.voicemap.domain.voice.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.chat.entity.ChatContext;
import org.ku.voicemap.domain.chat.reposiotry.ChatContextRepository;
import org.ku.voicemap.domain.voice.ai.AgentConnector;
import org.ku.voicemap.domain.voice.ai.gemini.config.GeminiProperties;
import org.ku.voicemap.domain.voice.ai.gemini.payload.BidiGenerateContentRealtimeInput;
import org.ku.voicemap.domain.voice.ai.gemini.payload.SetupMessage;
import org.ku.voicemap.domain.voice.outbound.ConversationOutboundService;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAgentConnector implements AgentConnector {

    private final WebSocketClient webSocketClient;
    private final VoiceSessionRepository sessionRepository;
    private final ChatContextRepository chatContextRepository;

    private final GeminiProperties geminiProperties;

    // Handler 생성 시에만 사용한다.
    private final ObjectMapper objectMapper;
    private final ConversationOutboundService outbound;

    @Override
    public void connect(String sessionId) {
        Runnable onDisconnect = () -> reconnect(sessionId);
        webSocketClient.execute(
                new GeminiAgentWebSocketHandler(sessionId, objectMapper, outbound, onDisconnect),
                geminiProperties.urls().agentWebSocketUrl() + "?key=" + geminiProperties.apiKey()
            ).thenAccept(agentConnection ->
                sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                    session.bindAgentConnection(agentConnection);
                    try {
                        String resumptionHandle = session.getResumptionHandle();
                        String systemInstruction = buildSystemInstruction(session.getChatId());
                        SetupMessage setupMessage = SetupMessage.create(systemInstruction, resumptionHandle);
                        agentConnection.sendMessage(new TextMessage(objectMapper.writeValueAsString(setupMessage)));
                    } catch (IOException e) {
                        log.error("[GeminiAgentConnector] Failed to send setup message for session: {}", sessionId, e);
                    }
                }))
            .exceptionally(e -> {
                log.error("[GeminiAgentConnector] Failed to connect to Gemini for session: {}", sessionId, e);
                sessionRepository.remove(sessionId);
                return null;
            });
    }

    private void reconnect(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            if (session.getResumptionHandle() == null) {
                log.info("[GeminiAgentConnector] No resumption handle for session: {}, skipping reconnect", sessionId);
                return;
            }
            if (!session.getClientConnection().isOpen()) {
                log.info("[GeminiAgentConnector] Client connection closed for session: {}, skipping reconnect", sessionId);
                return;
            }
            log.info("[GeminiAgentConnector] Reconnecting session: {} with resumption handle", sessionId);
            connect(sessionId);
        });
    }

    private String buildSystemInstruction(String chatId) {
        String baseInstruction = geminiProperties.instructions().agent();
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

    @Override
    public void sendAudio(String sessionId, String base64Audio) {
        WebSocketSession agentConnection = sessionRepository.findBySessionId(sessionId)
            .map(VoiceSession::getAgentConnection)
            .orElseThrow(() -> new IllegalArgumentException("Agent connection not found: " + sessionId));
        try {
            BidiGenerateContentRealtimeInput input = new BidiGenerateContentRealtimeInput(base64Audio);
            String payload = objectMapper.writeValueAsString(input);
            agentConnection.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.error("[GeminiAgentConnector] Failed to send audio, sessionId: {}", sessionId, e);
        }
    }

    @Override
    public void disconnect(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
            .map(VoiceSession::getAgentConnection)
            .ifPresent(agentConnection -> {
                try {
                    agentConnection.close();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to close agent connection: " + sessionId, e);
                }
            });
    }
}
