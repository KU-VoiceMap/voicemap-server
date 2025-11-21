package org.ku.voicemap.domain.script.dto;

import java.time.LocalDateTime;

public record ScriptCreateResponse(Long scriptId,
                                   boolean isAnswered,
                                   LocalDateTime createdAt,
                                   LocalDateTime answeredAt) {
}
