package org.ku.voicemap.ai.gemini.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.ai.chat.ChatTitleResult;
import org.ku.voicemap.ai.chat.DocumentResult;
import org.ku.voicemap.ai.chat.IdeaContextResult;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatClientTest {

    private MockRestServiceServer mockServer;
    private GeminiChatClient client;
    private GeminiProperties geminiProperties;

    private static final String CHAT_API_URL = "https://api.gemini.test/chat";
    private static final String DOCUMENT_API_URL = "https://api.gemini.test/document";
    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties(
            API_KEY,
            new GeminiProperties.Urls("ws://agent", DOCUMENT_API_URL, CHAT_API_URL),
            new GeminiProperties.Models("agent-model", "doc-model", "chat-model"),
            new GeminiProperties.SystemInstructions("agent-inst", "doc-inst", "chat-inst", "summarizer-inst")
        );

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new GeminiChatClient(restClient, geminiProperties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void generateTitle_success() {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"title\\": \\"테스트 제목\\"}"
                  }]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(CHAT_API_URL + "?key=" + API_KEY))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        ChatTitleResult result = client.generateTitle("system", "prompt");

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("테스트 제목");
    }

    @Test
    void summarizeContext_success() {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"coreIdea\\": \\"핵심 아이디어\\", \\"decisions\\": \\"결정사항\\", \\"currentPhase\\": \\"현재 단계\\", \\"unexplored\\": \\"미탐색\\"}"
                  }]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(CHAT_API_URL + "?key=" + API_KEY))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        IdeaContextResult result = client.summarizeContext("system", "prompt");

        assertThat(result).isNotNull();
        assertThat(result.coreIdea()).isEqualTo("핵심 아이디어");
        assertThat(result.decisions()).isEqualTo("결정사항");
        assertThat(result.currentPhase()).isEqualTo("현재 단계");
        assertThat(result.unexplored()).isEqualTo("미탐색");
    }

    @Test
    void generateDocument_success() {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"title\\": \\"문서 제목\\", \\"summary\\": \\"요약\\", \\"content\\": \\"내용\\", \\"keywords\\": [\\"키워드1\\", \\"키워드2\\"]}"
                  }]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(DOCUMENT_API_URL + "?key=" + API_KEY))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        DocumentResult result = client.generateDocument("system", "prompt", List.of("existing"));

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("문서 제목");
        assertThat(result.summary()).isEqualTo("요약");
        assertThat(result.content()).isEqualTo("내용");
        assertThat(result.keywords()).containsExactly("키워드1", "키워드2");
    }

    @Test
    void generateTitle_parsesNestedGeminiResponse() {
        String geminiResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"title\\": \\"중첩 파싱 검증\\"}"
                  }]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(CHAT_API_URL + "?key=" + API_KEY))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        ChatTitleResult result = client.generateTitle("system instruction", "user prompt");

        assertThat(result.title()).isEqualTo("중첩 파싱 검증");
    }
}
