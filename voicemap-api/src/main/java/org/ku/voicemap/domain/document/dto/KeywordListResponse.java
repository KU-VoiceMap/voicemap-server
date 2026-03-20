package org.ku.voicemap.domain.document.dto;

import java.util.List;

public record KeywordListResponse(
    List<KeywordSummary> keywords
) {

    public record KeywordSummary(
        Long keywordId,
        String name,
        long documentCount
    ) {
    }
}
