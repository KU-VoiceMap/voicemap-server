package org.ku.voicemap.api.chat.service.dto;

import java.util.List;

public record MemberChatsResponse(
    List<MemberChatResponse> chats
) {

    public record MemberChatResponse(String chatId, String title) {
    }
}
