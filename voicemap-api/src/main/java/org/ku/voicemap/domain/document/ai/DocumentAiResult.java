package org.ku.voicemap.domain.document.ai;

import java.util.List;

public record DocumentAiResult(
    String title,
    String summary,
    String content,
    List<String> keywords
) {
}
