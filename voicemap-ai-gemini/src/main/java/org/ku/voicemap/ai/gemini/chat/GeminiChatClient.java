package org.ku.voicemap.ai.gemini.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.ChatTitleResult;
import org.ku.voicemap.ai.chat.DocumentResult;
import org.ku.voicemap.ai.chat.IdeaContextResult;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.ku.voicemap.ai.gemini.payload.GeminiChatRequest;
import org.ku.voicemap.ai.gemini.payload.GeminiChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiChatClient implements AiChatClient {

    private static final Map<String, Object> TITLE_SCHEMA = Map.of(
        "type", "OBJECT",
        "properties", Map.of(
            "title", Map.of("type", "STRING")
        ),
        "required", List.of("title")
    );

    private static final Map<String, Object> CONTEXT_SCHEMA = Map.of(
        "type", "OBJECT",
        "properties", Map.of(
            "coreIdea", Map.of("type", "STRING"),
            "decisions", Map.of("type", "STRING"),
            "currentPhase", Map.of("type", "STRING"),
            "unexplored", Map.of("type", "STRING")
        ),
        "required", List.of("coreIdea", "decisions", "currentPhase", "unexplored")
    );

    private static final Map<String, Object> DOCUMENT_SCHEMA = Map.of(
        "type", "OBJECT",
        "properties", Map.of(
            "title", Map.of("type", "STRING"),
            "summary", Map.of("type", "STRING"),
            "content", Map.of("type", "STRING"),
            "keywords", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
        ),
        "required", List.of("title", "summary", "content", "keywords")
    );

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiChatClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    GeminiChatClient(RestClient restClient, GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatTitleResult generateTitle(String systemInstruction, String prompt) {
        String url = geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey();
        GeminiChatRequest request = GeminiChatRequest.of(systemInstruction, prompt, TITLE_SCHEMA);
        String text = callGemini(url, request).extractText();

        try {
            return objectMapper.readValue(text, ChatTitleResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse generateTitle response", e);
        }
    }

    @Override
    public IdeaContextResult summarizeContext(String systemInstruction, String prompt) {
        String url = geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey();
        GeminiChatRequest request = GeminiChatRequest.of(systemInstruction, prompt, CONTEXT_SCHEMA);
        String text = callGemini(url, request).extractText();

        try {
            return objectMapper.readValue(text, IdeaContextResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse summarizeContext response", e);
        }
    }

    @Override
    public DocumentResult generateDocument(String systemInstruction, String prompt, List<String> existingKeywords) {
        String url = geminiProperties.urls().documentApiUrl() + "?key=" + geminiProperties.apiKey();
        GeminiChatRequest request = GeminiChatRequest.of(systemInstruction, prompt, DOCUMENT_SCHEMA);
        String text = callGemini(url, request).extractText();

        try {
            return objectMapper.readValue(text, DocumentResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse generateDocument response", e);
        }
    }

    private GeminiChatResponse callGemini(String url, GeminiChatRequest request) {
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(GeminiChatResponse.class);
    }
}
