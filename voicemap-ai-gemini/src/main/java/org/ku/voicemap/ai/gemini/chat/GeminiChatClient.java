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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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
        return chat(geminiProperties.urls().chatApiUrl(), systemInstruction, prompt, TITLE_SCHEMA, ChatTitleResult.class);
    }

    @Override
    public IdeaContextResult summarizeContext(String systemInstruction, String prompt) {
        return chat(geminiProperties.urls().chatApiUrl(), systemInstruction, prompt, CONTEXT_SCHEMA, IdeaContextResult.class);
    }

    @Override
    public DocumentResult generateDocument(String systemInstruction, String prompt, List<String> existingKeywords) {
        return chat(geminiProperties.urls().documentApiUrl(), systemInstruction, prompt, DOCUMENT_SCHEMA, DocumentResult.class);
    }

    private <T> T chat(String baseUrl, String systemInstruction, String prompt,
                       Map<String, Object> schema, Class<T> resultType) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("key", geminiProperties.apiKey())
            .build()
            .toUri();
        GeminiChatRequest request = GeminiChatRequest.of(systemInstruction, prompt, schema);
        GeminiChatResponse response = restClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(GeminiChatResponse.class);

        try {
            return objectMapper.readValue(response.extractText(), resultType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse " + resultType.getSimpleName() + " response", e);
        }
    }
}
