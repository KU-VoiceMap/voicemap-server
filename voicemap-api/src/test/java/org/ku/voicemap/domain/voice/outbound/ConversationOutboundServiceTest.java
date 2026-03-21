package org.ku.voicemap.domain.voice.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.ChatTitleResult;
import org.ku.voicemap.ai.realtime.AiRole;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.config.AiInstructionProperties.Instructions;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.ku.voicemap.domain.voice.session.VoiceSession;
import org.ku.voicemap.domain.voice.session.VoiceSessionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class ConversationOutboundServiceTest {

    @Mock
    private VoiceSessionRepository sessionRepository;

    @Mock
    private ScriptRepository scriptRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AiInstructionProperties aiInstructionProperties;

    @Mock
    private ObjectMapper objectMapper;

    private ConversationOutboundService service;

    private static final String SESSION_ID = "test-session-id";
    private static final String CHAT_ID = "test-chat-id";
    private static final String MEMBER_NUMBER = "test-member-123";
    private static final String CHAT_INSTRUCTION = "chat instruction";

    @BeforeEach
    void setUp() {
        service = new ConversationOutboundService(
            sessionRepository, scriptRepository, chatRepository,
            aiChatClient, aiInstructionProperties, objectMapper
        );
    }

    @Test
    void onSessionReady_delegates_to_sendSessionReady() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.onSessionReady(SESSION_ID);

        verify(wsSession).sendMessage(any());
    }

    @Test
    void onTranscript_maps_USER_role_and_sends() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.onTranscript(SESSION_ID, AiRole.USER, "Hello");

        String transcript = voiceSession.getTranscript(ConversationRole.USER);
        assertThat(transcript).isEqualTo("Hello");
    }

    @Test
    void onTranscript_maps_AGENT_role_and_sends() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.onTranscript(SESSION_ID, AiRole.AGENT, "I am an agent");

        String transcript = voiceSession.getTranscript(ConversationRole.AGENT);
        assertThat(transcript).isEqualTo("I am an agent");
    }

    @Test
    void onTurnComplete_saves_script_and_generates_title_on_first_turn() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        voiceSession.appendTranscript(ConversationRole.USER, "My question");
        voiceSession.appendTranscript(ConversationRole.AGENT, "My answer");
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of());

        Instructions instructions = new Instructions("agent", "doc", CHAT_INSTRUCTION, "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);
        when(aiChatClient.generateTitle(eq(CHAT_INSTRUCTION), anyString()))
            .thenReturn(new ChatTitleResult("Generated Title"));

        Chat chat = mock(Chat.class);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        service.onTurnComplete(SESSION_ID);

        verify(scriptRepository).save(any(Script.class));
        verify(aiChatClient).generateTitle(eq(CHAT_INSTRUCTION), anyString());
        verify(chat).updateTitle("Generated Title");
    }

    @Test
    void onTurnComplete_does_not_generate_title_on_subsequent_turns() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        voiceSession.appendTranscript(ConversationRole.USER, "Second question");
        voiceSession.appendTranscript(ConversationRole.AGENT, "Second answer");
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Script existingScript = mock(Script.class);
        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of(existingScript));

        service.onTurnComplete(SESSION_ID);

        verify(aiChatClient, never()).generateTitle(anyString(), anyString());
    }

    @Test
    void onInterrupted_delegates_to_sendInterrupted() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.onInterrupted(SESSION_ID);

        verify(wsSession).sendMessage(any());
    }

    @Test
    void onAudioOutput_delegates_to_sendAudioOutput() throws Exception {
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        VoiceSession voiceSession = new VoiceSession(MEMBER_NUMBER, CHAT_ID, wsSession);
        when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(voiceSession));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.onAudioOutput(SESSION_ID, "base64audio==");

        verify(wsSession).sendMessage(any());
    }
}
