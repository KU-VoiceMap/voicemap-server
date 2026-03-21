package org.ku.voicemap.domain.document.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.document.ai.DocumentAiClient;
import org.ku.voicemap.domain.document.ai.DocumentAiResult;
import org.ku.voicemap.domain.voice.ai.gemini.config.GeminiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiDocumentAiClient implements DocumentAiClient {

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiDocumentAiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentAiResult generate(String conversationText, List<String> existingKeywords) {
        Map<String, Object> requestBody = buildRequest(conversationText, existingKeywords);

        String responseJson = restClient.post()
                .uri(geminiProperties.urls().documentApiUrl() + "?key=" + geminiProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseResponse(responseJson);
    }

    private Map<String, Object> buildRequest(String conversationText, List<String> existingKeywords) {
        String prompt = buildPrompt(conversationText, existingKeywords);

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", geminiProperties.instructions().document()))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", buildResponseSchema()
                )
        );
    }

    private String buildPrompt(String conversationText, List<String> existingKeywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 사용자와 AI의 대화 기록입니다. 이 대화를 바탕으로 문서를 작성하고 키워드를 추출해주세요.\n\n");

        if (!existingKeywords.isEmpty()) {
            sb.append("[기존 키워드 목록]: ").append(String.join(", ", existingKeywords)).append("\n");
            sb.append("※ 가능하면 기존 키워드를 재사용하고, 새로운 개념만 신규 키워드로 추출하세요.\n\n");
        }

        sb.append("[대화 기록]\n").append(conversationText);
        return sb.toString();
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "title", Map.of("type", "STRING"),
                        "summary", Map.of("type", "STRING"),
                        "content", Map.of("type", "STRING"),
                        "keywords", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING")
                        )
                ),
                "required", List.of("title", "summary", "content", "keywords")
        );
    }

    private DocumentAiResult parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode result = objectMapper.readTree(text);
            String title = result.path("title").asText();
            String summary = result.path("summary").asText();
            String content = result.path("content").asText();
            List<String> keywords = objectMapper.convertValue(
                    result.path("keywords"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new DocumentAiResult(title, summary, content, keywords);
        } catch (Exception e) {
            log.error("[GeminiDocumentAiClient] Failed to parse Gemini response", e);
            throw new RuntimeException("AI 응답 파싱에 실패했습니다.", e);
        }
    }


}
