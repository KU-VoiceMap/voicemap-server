package org.ku.voicemap.domain.document.ai;

import java.util.List;

public interface DocumentAiClient {

    DocumentAiResult generate(String conversationText, List<String> existingKeywords);
}
