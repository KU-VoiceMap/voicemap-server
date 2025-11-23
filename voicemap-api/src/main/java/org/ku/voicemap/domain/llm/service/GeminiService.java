package org.ku.voicemap.domain.LLM.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.LLM.dto.ScriptSummaryDto;
import org.ku.voicemap.domain.LLM.dto.ScriptToLLMDto;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
public class GeminiService implements LlmService{

    private final Client client;
    private final ObjectMapper objectMapper;
    private static final String toSummary="Summarize the conversation";
    private static final String toKeyword="extract important keywords from the content";
    private static final String howTo="This is the conversation we've had up to this point, so we can summarize it";

    @Override
    public ScriptSummaryDto summaryScript(ScriptToLLMDto scripts){

        Schema schema = Schema.builder()
            .type(Known.OBJECT)
            .properties(Map.of(
                "summary", Schema.builder()
                    .type(Known.STRING)
                    .description(toSummary)
                    .build(),
                "keywords", Schema.builder()
                    .type(Known.ARRAY)
                    .items(Schema.builder()
                        .type(Known.STRING)
                        .build())
                    .description(toKeyword)
                    .build()
            ))
            .required(Arrays.asList("summary", "keywords"))
            .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
            .build();
        GenerateContentResponse response = client.models.generateContent(
            "gemini-2.5-flash",
            howTo + scripts,
            config
        );
        String jsonString = response.text();
        try {
            return objectMapper.readValue(jsonString, ScriptSummaryDto.class);
        } catch (Exception e) {
            throw new RuntimeException(jsonString, e);
        }

    }
}
