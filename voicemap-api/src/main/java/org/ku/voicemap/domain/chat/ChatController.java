package org.ku.voicemap.domain.chat;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.config.AuthenticatedMember;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse;
import org.ku.voicemap.domain.chat.service.ChatService;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chats")
    public CreateChatResponse createChat(@AuthenticatedMember String memberNumber, @RequestBody CreateChatRequest request) {
        return chatService.createChat(memberNumber, request);
    }

    @GetMapping("/chats")
    public MemberChatsResponse getMemberChats(@AuthenticatedMember String memberNumber) {
        return chatService.getMemberChats(memberNumber);
    }
}
