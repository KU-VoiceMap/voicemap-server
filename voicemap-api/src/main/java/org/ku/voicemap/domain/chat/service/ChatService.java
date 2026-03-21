package org.ku.voicemap.domain.chat.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.chat.dto.ChatDetailsResponse;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse.MemberChatResponse;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
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
    private final ChatTitleSummarizer chatTitleSummarizer;

    @Transactional
    public CreateChatResponse createChat(String memberNumber, CreateChatRequest request) {
        Chat chat = chatRepository.save(new Chat(memberNumber, request.question()));

        return new CreateChatResponse(
            chat.getId(),
            chatTitleSummarizer.summarize(request.question())
        );
    }

    @Transactional(readOnly = true)
    public MemberChatsResponse getMemberChats(String memberNumber) {
        List<Chat> chats = chatRepository.findAllByMemberNumber(memberNumber);
        return new MemberChatsResponse(
            chats.stream()
                .map(chat -> new MemberChatResponse(chat.getId(), chat.getTitle()))
                .toList()
        );
    }

    public ChatDetailsResponse getChatDetail(String memberNumber, String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅이 존재하지 않습니다."));
        if (!chat.getMemberNumber().equals(memberNumber)) {
            throw new IllegalArgumentException("해당 채팅이 존재하지 않습니다.");
        }

        List<Script> scripts = scriptRepository.findAllByChatId(chatId);
        return ChatDetailsResponse.from(scripts);
    }
}
