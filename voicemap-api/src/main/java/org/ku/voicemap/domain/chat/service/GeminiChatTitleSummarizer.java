package org.ku.voicemap.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.voice.ai.gemini.payload.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiChatTitleSummarizer implements ChatTitleSummarizer {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final int TITLE_MAX_LENGTH = 30;

    private static final String SYSTEM_INSTRUCTION = """
        당신은 대화 내용을 바탕으로 짧고 직관적인 대화 제목을 생성하는 AI입니다.

        규칙:
        1. 제목은 한국어로 작성하세요.
        2. 제목은 최대 30자 이내로 간결하게 작성하세요.
        3. 대화의 핵심 주제를 담아야 합니다.
        4. 불필요한 조사나 서술어 없이, 명사구 중심으로 작성하세요.
        5. 따옴표, 마침표 등 특수문자를 포함하지 마세요.
        """;

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public GeminiChatTitleSummarizer(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String summarize(String content) {
        try {
            Map<String, Object> requestBody = buildRequest(content);
            String responseJson = restClient.post()
                .uri(GEMINI_URL + "?key=" + aiProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            return parseTitle(responseJson);
        } catch (Exception e) {
            log.error("[GeminiChatTitleSummarizer] Failed to generate title, falling back to truncation", e);
            return fallbackTitle(content);
        }
    }

    private Map<String, Object> buildRequest(String content) {
        String prompt = "다음 대화 내용을 바탕으로 대화 제목을 생성해주세요.\n\n" + content;

        return Map.of(
            "systemInstruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
            ),
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                        "title", Map.of("type", "STRING")
                    ),
                    "required", List.of("title")
                )
            )
        );
    }

    private String parseTitle(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

            JsonNode result = objectMapper.readTree(text);
            String title = result.path("title").asText();

            if (title.length() > TITLE_MAX_LENGTH) {
                return title.substring(0, TITLE_MAX_LENGTH);
            }
            return title;
        } catch (Exception e) {
            log.error("[GeminiChatTitleSummarizer] Failed to parse response", e);
            throw new RuntimeException("AI 응답 파싱에 실패했습니다.", e);
        }
    }

    private String fallbackTitle(String content) {
        String firstLine = content.lines().findFirst().orElse(content);
        if (firstLine.length() <= TITLE_MAX_LENGTH) {
            return firstLine;
        }
        return firstLine.substring(0, TITLE_MAX_LENGTH) + "...";
    }
}
