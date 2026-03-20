package org.ku.voicemap.domain.document.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(
    @NotBlank String chatId
) {
}
