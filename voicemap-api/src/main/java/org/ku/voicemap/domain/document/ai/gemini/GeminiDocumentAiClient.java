package org.ku.voicemap.domain.document.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.document.ai.DocumentAiClient;
import org.ku.voicemap.domain.document.ai.DocumentAiResult;
import org.ku.voicemap.domain.voice.ai.gemini.payload.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiDocumentAiClient implements DocumentAiClient {

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public GeminiDocumentAiClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentAiResult generate(String conversationText, List<String> existingKeywords) {
        Map<String, Object> requestBody = buildRequest(conversationText, existingKeywords);

        String responseJson = restClient.post()
                .uri(GEMINI_URL + "?key=" + aiProperties.apiKey())
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
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
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

    private static final String SYSTEM_INSTRUCTION = """
            당신은 대화 내용을 분석하여 구조화된 문서를 작성하는 AI입니다.
            
            규칙:
            1. 문서 제목(title)은 핵심 주제를 간결하게 나타내세요.
            2. 요약(summary)은 대화의 핵심 내용을 2~3문장으로 간결하게 요약하세요.
            3. 본문(content)은 대화의 핵심 내용을 자연스러운 문서로 상세하게 정리하세요.
            4. 키워드(keywords)는 5~10개를 추출하세요.
            5. 기존 키워드 목록이 주어지면, 같은 개념은 기존 키워드를 재사용하세요.
            6. 키워드는 소문자로 통일하세요.
            """;
}
