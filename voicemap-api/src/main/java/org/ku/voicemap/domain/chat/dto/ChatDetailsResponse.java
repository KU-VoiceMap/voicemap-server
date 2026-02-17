package org.ku.voicemap.domain.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.ku.voicemap.domain.script.Script;

public record ChatDetailsResponse(
    List<ChatDetailResponse> scripts
) {

    public record ChatDetailResponse(
        String question,
        String answer,
        LocalDateTime createdAt
    ) {}

    public static ChatDetailsResponse from(List<Script> scripts) {
        return new ChatDetailsResponse(
            scripts.stream()
                .map(script -> new ChatDetailResponse(
                    script.getQuestion(),
                    script.isAnswered() ? script.getAnswer() : null,
                    script.getCreatedAt()
                ))
                .toList()
        );
    }
}
