package org.ku.voicemap.domain.chat.dto;

import java.util.List;

public record MemberChatsResponse(
    List<MemberChatResponse> chats
) {

    public record MemberChatResponse(String chatId, String title) {
    }
}
