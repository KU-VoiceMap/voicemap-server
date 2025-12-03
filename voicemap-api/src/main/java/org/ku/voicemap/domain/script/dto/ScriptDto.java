package org.ku.voicemap.domain.script.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ku.voicemap.domain.script.entity.Script;

public record ScriptDto(Long id,
                        UUID chatId,
                        String question,
                        String answer,
                        LocalDateTime createdAt) {

    public static ScriptDto toDto(Script script) {
        return new ScriptDto(
            script.getId(),
            script.getChatId(),
            script.getQuestion(),
            script.getAnswer(),
            script.getCreatedAt()
        );
    }
}
