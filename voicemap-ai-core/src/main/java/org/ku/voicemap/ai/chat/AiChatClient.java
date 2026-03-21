package org.ku.voicemap.ai.chat;

import java.util.List;

public interface AiChatClient {

    ChatTitleResult generateTitle(String systemInstruction, String prompt);

    IdeaContextResult summarizeContext(String systemInstruction, String prompt);

    DocumentResult generateDocument(String systemInstruction, String prompt, List<String> existingKeywords);
}
