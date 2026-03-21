package org.ku.voicemap.ai.gemini.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.ChatTitleResult;
import org.ku.voicemap.ai.chat.DocumentResult;
import org.ku.voicemap.ai.chat.IdeaContextResult;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiChatClient implements AiChatClient {

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

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
        Map<String, Object> responseSchema = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                "title", Map.of("type", "STRING")
            ),
            "required", List.of("title")
        );

        String url = geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey();
        String responseJson = callGemini(url, systemInstruction, prompt, responseSchema);
        String text = extractText(responseJson);

        try {
            JsonNode node = objectMapper.readTree(text);
            String title = node.get("title").asText();
            return new ChatTitleResult(title);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse generateTitle response", e);
        }
    }

    @Override
    public IdeaContextResult summarizeContext(String systemInstruction, String prompt) {
        Map<String, Object> responseSchema = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                "coreIdea", Map.of("type", "STRING"),
                "decisions", Map.of("type", "STRING"),
                "currentPhase", Map.of("type", "STRING"),
                "unexplored", Map.of("type", "STRING")
            ),
            "required", List.of("coreIdea", "decisions", "currentPhase", "unexplored")
        );

        String url = geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey();
        String responseJson = callGemini(url, systemInstruction, prompt, responseSchema);
        String text = extractText(responseJson);

        try {
            JsonNode node = objectMapper.readTree(text);
            String coreIdea = node.get("coreIdea").asText();
            String decisions = node.get("decisions").asText();
            String currentPhase = node.get("currentPhase").asText();
            String unexplored = node.get("unexplored").asText();
            return new IdeaContextResult(coreIdea, decisions, currentPhase, unexplored);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse summarizeContext response", e);
        }
    }

    @Override
    public DocumentResult generateDocument(String systemInstruction, String prompt, List<String> existingKeywords) {
        Map<String, Object> responseSchema = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                "title", Map.of("type", "STRING"),
                "summary", Map.of("type", "STRING"),
                "content", Map.of("type", "STRING"),
                "keywords", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
            ),
            "required", List.of("title", "summary", "content", "keywords")
        );

        String url = geminiProperties.urls().documentApiUrl() + "?key=" + geminiProperties.apiKey();
        String responseJson = callGemini(url, systemInstruction, prompt, responseSchema);
        String text = extractText(responseJson);

        try {
            JsonNode node = objectMapper.readTree(text);
            String title = node.get("title").asText();
            String summary = node.get("summary").asText();
            String content = node.get("content").asText();
            List<String> keywords = objectMapper.convertValue(
                node.get("keywords"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return new DocumentResult(title, summary, content, keywords);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse generateDocument response", e);
        }
    }

    private String callGemini(String url, String systemInstruction, String prompt, Map<String, Object> responseSchema) {
        Map<String, Object> requestBody = buildRequestBody(systemInstruction, prompt, responseSchema);

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Gemini request body", e);
        }
    }

    private Map<String, Object> buildRequestBody(String systemInstruction, String prompt, Map<String, Object> responseSchema) {
        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of(
            "parts", List.of(Map.of("text", systemInstruction))
        ));
        body.put("contents", List.of(Map.of(
            "parts", List.of(Map.of("text", prompt))
        )));
        body.put("generationConfig", Map.of(
            "responseMimeType", "application/json",
            "responseSchema", responseSchema
        ));
        return body;
    }

    String extractText(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
