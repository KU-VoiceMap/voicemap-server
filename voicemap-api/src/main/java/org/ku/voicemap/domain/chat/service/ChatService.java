package org.ku.voicemap.domain.chat.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse.MemberChatResponse;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatTitleSummarizer chatTitleSummarizer;

    @Transactional
    public CreateChatResponse createChat(String memberNumber, CreateChatRequest request) {
        Chat chat = chatRepository.save(new Chat(memberNumber, request.question()));

        return new CreateChatResponse(
            chat.getId(),
            chatTitleSummarizer.summarize(request.question()) // TODO: 제목 생성 외부요청한다면 별도로 분리
        );
    }

    public MemberChatsResponse getMemberChats(String memberNumber) {
        List<Chat> chats = chatRepository.findAllByMemberNumber(memberNumber);
        return new MemberChatsResponse(
            chats.stream()
                .map(chat -> new MemberChatResponse(chat.getId(), chat.getTitle()))
                .toList()
        );
    }
}
