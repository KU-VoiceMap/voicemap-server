package org.ku.voicemap.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.voice.ai.gemini.config.GeminiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiIdeaContextSummarizer implements IdeaContextSummarizer {

    private static final String SYSTEM_INSTRUCTION = """
            당신은 아이디어 빌딩 대화의 진행 상태를 요약하는 AI입니다.

            규칙:
            1. 반드시 한국어로 작성하세요.
            2. 아래 형식을 정확히 따르세요.
            3. 300자 이내로 간결하게 작성하세요.
            4. 이전 요약이 있으면 새 대화 내용을 반영하여 갱신하세요.
            5. 이전 요약이 없으면 대화 내용만으로 새로 작성하세요.
            """;

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiIdeaContextSummarizer(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String summarize(String previousContext, String conversation) {
        try {
            Map<String, Object> requestBody = buildRequest(previousContext, conversation);
            String responseJson = restClient.post()
                .uri(geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseContext(responseJson);
        } catch (Exception e) {
            log.error("[GeminiIdeaContextSummarizer] Failed to summarize idea context", e);
            return previousContext;
        }
    }

    private Map<String, Object> buildRequest(String previousContext, String conversation) {
        StringBuilder prompt = new StringBuilder();

        if (previousContext != null && !previousContext.isBlank()) {
            prompt.append("## 이전 아이디어 진행 상태\n");
            prompt.append(previousContext);
            prompt.append("\n\n");
        }

        prompt.append("## 새로운 대화 내용\n");
        prompt.append(conversation);
        prompt.append("\n\n위 내용을 바탕으로 아이디어 진행 상태를 정리하세요.");

        return Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
            ),
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt.toString())))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                        "coreIdea", Map.of("type", "STRING"),
                        "decisions", Map.of("type", "STRING"),
                        "currentPhase", Map.of("type", "STRING"),
                        "unexplored", Map.of("type", "STRING")
                    ),
                    "required", List.of("coreIdea", "decisions", "currentPhase", "unexplored")
                )
            )
        );
    }

    private String parseContext(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

            JsonNode result = objectMapper.readTree(text);

            return "### 핵심 아이디어\n" + result.path("coreIdea").asText()
                + "\n\n### 내려진 결정들\n" + result.path("decisions").asText()
                + "\n\n### 현재 단계\n" + result.path("currentPhase").asText()
                + "\n\n### 아직 탐색되지 않은 영역\n" + result.path("unexplored").asText();
        } catch (Exception e) {
            log.error("[GeminiIdeaContextSummarizer] Failed to parse response", e);
            throw new RuntimeException("아이디어 컨텍스트 파싱에 실패했습니다.", e);
        }
    }
}
