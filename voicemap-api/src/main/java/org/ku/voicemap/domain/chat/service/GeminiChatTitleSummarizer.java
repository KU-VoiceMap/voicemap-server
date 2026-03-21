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
public class GeminiChatTitleSummarizer implements ChatTitleSummarizer {

    private static final int TITLE_MAX_LENGTH = 30;

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public GeminiChatTitleSummarizer(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String summarize(String content) {
        try {
            Map<String, Object> requestBody = buildRequest(content);
            String responseJson = restClient.post()
                .uri(geminiProperties.urls().chatApiUrl() + "?key=" + geminiProperties.apiKey())
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
                "parts", List.of(Map.of("text", geminiProperties.instructions().chat()))
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
