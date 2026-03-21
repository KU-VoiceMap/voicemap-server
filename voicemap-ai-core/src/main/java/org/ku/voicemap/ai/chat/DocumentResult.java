package org.ku.voicemap.ai.chat;

import java.util.List;

public record DocumentResult(
    String title,
    String summary,
    String content,
    List<String> keywords
) {
}
