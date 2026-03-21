package org.ku.voicemap.domain.voice.inbound;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.realtime.AiRealtimeClient;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.config.AiInstructionProperties.Instructions;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.entity.ChatContext;
import org.ku.voicemap.domain.chat.repository.ChatContextRepository;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.inbound.payload.AudioInputPayload;
import org.ku.voicemap.domain.voice.inbound.payload.SessionInitPayload;
import org.ku.voicemap.domain.voice.outbound.ConversationOutboundService;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class ConversationInboundServiceTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatContextRepository chatContextRepository;

    @Mock
    private ScriptRepository scriptRepository;

    @Mock
    private VoiceSessionRepository sessionRepository;

    @Mock
    private AiRealtimeClient aiRealtimeClient;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AiInstructionProperties aiInstructionProperties;

    @Mock
    private ConversationOutboundService outbound;

    private ConversationInboundService service;

    private static final String MEMBER_NUMBER = "member-123";
    private static final String CHAT_ID = "chat-456";
    private static final String TOKEN = "jwt-token";
    private static final String AGENT_INSTRUCTION = "agent instruction text";

    @BeforeEach
    void setUp() {
        service = new ConversationInboundService(
            tokenProvider, chatRepository, chatContextRepository,
            scriptRepository, sessionRepository, aiRealtimeClient,
            aiChatClient, aiInstructionProperties, outbound
        );
    }

    @Test
    void initializeSession_creates_new_chat_and_connects_ai() {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        SessionInitPayload payload = new SessionInitPayload(TOKEN, null);

        when(tokenProvider.extractMemberNumber(TOKEN)).thenReturn(MEMBER_NUMBER);
        Chat newChat = mock(Chat.class);
        when(newChat.getId()).thenReturn(CHAT_ID);
        when(chatRepository.save(any(Chat.class))).thenReturn(newChat);
        when(chatContextRepository.findFirstByChatIdOrderByCreatedAtDesc(CHAT_ID)).thenReturn(Optional.empty());

        Instructions instructions = new Instructions(AGENT_INSTRUCTION, "doc", "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        service.initializeSession(wsSession, payload);

        verify(sessionRepository).save(any(VoiceSession.class));
        verify(aiRealtimeClient).connect(anyString(), eq(AGENT_INSTRUCTION), eq(outbound));
    }

    @Test
    void initializeSession_with_existing_chatId_uses_that_chat() {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        SessionInitPayload payload = new SessionInitPayload(TOKEN, CHAT_ID);

        when(tokenProvider.extractMemberNumber(TOKEN)).thenReturn(MEMBER_NUMBER);
        Chat existingChat = mock(Chat.class);
        when(existingChat.getId()).thenReturn(CHAT_ID);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(existingChat));
        when(chatContextRepository.findFirstByChatIdOrderByCreatedAtDesc(CHAT_ID)).thenReturn(Optional.empty());

        Instructions instructions = new Instructions(AGENT_INSTRUCTION, "doc", "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        service.initializeSession(wsSession, payload);

        verify(chatRepository).findById(CHAT_ID);
        verify(aiRealtimeClient).connect(anyString(), eq(AGENT_INSTRUCTION), eq(outbound));
    }

    @Test
    void initializeSession_includes_context_in_system_instruction_when_available() {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        SessionInitPayload payload = new SessionInitPayload(TOKEN, null);

        when(tokenProvider.extractMemberNumber(TOKEN)).thenReturn(MEMBER_NUMBER);
        Chat newChat = mock(Chat.class);
        when(newChat.getId()).thenReturn(CHAT_ID);
        when(chatRepository.save(any(Chat.class))).thenReturn(newChat);

        ChatContext chatContext = mock(ChatContext.class);
        when(chatContext.getContext()).thenReturn("previous context");
        when(chatContextRepository.findFirstByChatIdOrderByCreatedAtDesc(CHAT_ID)).thenReturn(Optional.of(chatContext));

        Instructions instructions = new Instructions(AGENT_INSTRUCTION, "doc", "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        ArgumentCaptor<String> instructionCaptor = ArgumentCaptor.forClass(String.class);
        service.initializeSession(wsSession, payload);

        verify(aiRealtimeClient).connect(anyString(), instructionCaptor.capture(), eq(outbound));
        String capturedInstruction = instructionCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(capturedInstruction).contains(AGENT_INSTRUCTION);
        org.assertj.core.api.Assertions.assertThat(capturedInstruction).contains("previous context");
    }

    @Test
    void handleAudioInput_sends_audio_to_realtime_client() {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        AudioInputPayload payload = new AudioInputPayload("base64audio==");

        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findByClientConnection(wsSession)).thenReturn(Optional.of(voiceSession));

        service.handleAudioInput(wsSession, payload);

        verify(aiRealtimeClient).sendAudio(voiceSession.getSessionId(), "base64audio==");
    }

    @Test
    void disconnectSession_disconnects_realtime_client_and_removes_session() {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findByClientConnection(wsSession)).thenReturn(Optional.of(voiceSession));

        service.disconnectSession(wsSession);

        verify(aiRealtimeClient).disconnect(voiceSession.getSessionId());
        verify(sessionRepository).remove(voiceSession.getSessionId());
    }
}
