package org.ku.voicemap.ai.gemini.payload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetupMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createWithoutResumptionHandle() throws Exception {
        SetupMessage message = SetupMessage.create("models/gemini-test", "test instruction");
        String json = objectMapper.writeValueAsString(message);
        JsonNode root = objectMapper.readTree(json);

        JsonNode setup = root.get("setup");
        assertThat(setup.get("model").asText()).isEqualTo("models/gemini-test");
        assertThat(setup.get("systemInstruction").get("parts").get(0).get("text").asText())
            .isEqualTo("test instruction");
        assertThat(setup.get("generationConfig").get("responseModalities").get(0).asText())
            .isEqualTo("AUDIO");
    }

    @Test
    void createWithResumptionHandle() throws Exception {
        SetupMessage message = SetupMessage.create("models/gemini-test", "instruction", "handle-123");
        String json = objectMapper.writeValueAsString(message);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("setup").get("sessionResumption").get("handle").asText())
            .isEqualTo("handle-123");
    }

    @Test
    void nullResumptionHandleIsExcludedFromJson() throws Exception {
        SetupMessage message = SetupMessage.create("models/gemini-test", "instruction", null);
        String json = objectMapper.writeValueAsString(message);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("setup").get("sessionResumption").has("handle")).isFalse();
    }
}
