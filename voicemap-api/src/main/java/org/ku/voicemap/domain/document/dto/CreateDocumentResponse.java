package org.ku.voicemap.domain.document.dto;

import java.util.List;

public record CreateDocumentResponse(
    String documentId,
    String title,
    String summary,
    List<String> keywords
) {
}
