package org.ku.voicemap.domain.document.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDetailResponse(
    String documentId,
    String chatId,
    String title,
    String summary,
    String content,
    List<String> keywords,
    LocalDateTime createdAt
) {
}
