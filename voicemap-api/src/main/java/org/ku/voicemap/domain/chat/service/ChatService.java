package org.ku.voicemap.domain.chat.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.domain.chat.dto.ChatDetailsResponse;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse.MemberChatResponse;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ScriptRepository scriptRepository;
    private final AiChatClient aiChatClient;
    private final AiInstructionProperties aiInstructionProperties;

    @Transactional
    public CreateChatResponse createChat(String memberNumber, CreateChatRequest request) {
        Chat chat = chatRepository.save(new Chat(memberNumber, request.question()));

        String instruction = aiInstructionProperties.instructions().chat();
        String title = aiChatClient.generateTitle(instruction, request.question()).title();

        return new CreateChatResponse(
            chat.getId(),
            title
        );
    }

    @Transactional(readOnly = true)
    public MemberChatsResponse getMemberChats(String memberNumber) {
        List<Chat> chats = chatRepository.findAllByMemberNumberOrderByLastInteractedAtDesc(memberNumber);
        return new MemberChatsResponse(
            chats.stream()
                .map(chat -> new MemberChatResponse(chat.getId(), chat.getTitle(), chat.getLastInteractedAt()))
                .toList()
        );
    }

    public ChatDetailsResponse getChatDetail(String memberNumber, String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅이 존재하지 않습니다."));
        if (!chat.getMemberNumber().equals(memberNumber)) {
            throw new IllegalArgumentException("해당 채팅이 존재하지 않습니다.");
        }

        List<Script> scripts = scriptRepository.findAllByChatIdOrderByCreatedAt(chatId);
        return ChatDetailsResponse.from(scripts);
    }
}
