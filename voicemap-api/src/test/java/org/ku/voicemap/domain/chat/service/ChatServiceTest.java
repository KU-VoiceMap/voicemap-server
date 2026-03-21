package org.ku.voicemap.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.ChatTitleResult;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.config.AiInstructionProperties.Instructions;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ScriptRepository scriptRepository;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AiInstructionProperties aiInstructionProperties;

    private ChatService chatService;

    private static final String MEMBER_NUMBER = "member-123";
    private static final String CHAT_INSTRUCTION = "chat instruction";

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatRepository, scriptRepository, aiChatClient, aiInstructionProperties);
    }

    @Test
    void createChat_uses_aiChatClient_to_generate_title() {
        CreateChatRequest request = new CreateChatRequest("What is the meaning of life?");
        Chat savedChat = mock(Chat.class);
        when(savedChat.getId()).thenReturn("chat-1");
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

        Instructions instructions = new Instructions("agent", "doc", CHAT_INSTRUCTION, "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);
        when(aiChatClient.generateTitle(eq(CHAT_INSTRUCTION), eq("What is the meaning of life?")))
            .thenReturn(new ChatTitleResult("삶의 의미 탐구"));

        CreateChatResponse response = chatService.createChat(MEMBER_NUMBER, request);

        assertThat(response.title()).isEqualTo("삶의 의미 탐구");
        verify(aiChatClient).generateTitle(CHAT_INSTRUCTION, "What is the meaning of life?");
    }

    @Test
    void createChat_passes_question_as_prompt_to_aiChatClient() {
        String question = "AI와 인간의 협업";
        CreateChatRequest request = new CreateChatRequest(question);
        Chat savedChat = mock(Chat.class);
        when(savedChat.getId()).thenReturn("chat-2");
        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

        Instructions instructions = new Instructions("agent", "doc", CHAT_INSTRUCTION, "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);
        when(aiChatClient.generateTitle(CHAT_INSTRUCTION, question))
            .thenReturn(new ChatTitleResult("AI와 인간 협업 탐구"));

        chatService.createChat(MEMBER_NUMBER, request);

        verify(aiChatClient).generateTitle(CHAT_INSTRUCTION, question);
    }
}
