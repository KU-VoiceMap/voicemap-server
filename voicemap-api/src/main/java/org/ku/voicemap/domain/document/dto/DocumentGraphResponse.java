package org.ku.voicemap.domain.document.dto;

import java.util.List;

public record DocumentGraphResponse(
    List<NodeResponse> nodes,
    List<EdgeResponse> edges
) {

    public record NodeResponse(
        String id,
        String type,
        String title,
        String name
    ) {
    }

    public record EdgeResponse(
        String documentId,
        Long keywordId
    ) {
    }
}
