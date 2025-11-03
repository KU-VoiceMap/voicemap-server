package org.ku.voicemap.api.chat.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.api.chat.service.dto.CreateChatRequest;
import org.ku.voicemap.api.chat.service.dto.CreateChatResponse;
import org.ku.voicemap.api.chat.service.dto.MemberChatsResponse;
import org.ku.voicemap.api.chat.service.dto.MemberChatsResponse.MemberChatResponse;
import org.ku.voicemap.domain.Chat;
import org.ku.voicemap.domain.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatTitleSummarizer chatTitleSummarizer;

    @Transactional
    public CreateChatResponse createChat(long memberId, CreateChatRequest request) {
        Chat chat = chatRepository.save(new Chat(memberId, request.question()));

        return new CreateChatResponse(
            chat.getId(),
            chatTitleSummarizer.summarize(request.question())
        );
    }

    public MemberChatsResponse getMemberChats(long memberId) {
        List<Chat> chats = chatRepository.findAllByMemberId(memberId);
        return new MemberChatsResponse(
            chats.stream()
                .map(chat -> new MemberChatResponse(chat.getId(), chat.getTitle()))
                .toList()
        );
    }
}
