package org.ku.voicemap.domain.document.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentListResponse(
    List<DocumentSummary> documents
) {

    public record DocumentSummary(
        String documentId,
        String title,
        String summary,
        String chatId,
        LocalDateTime createdAt
    ) {
    }
}
