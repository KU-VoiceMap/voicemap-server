package org.ku.voicemap.domain.LLM;

import com.google.api.client.util.Value;
import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GeminiConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Bean
    public Client GeminiConfig(){
        return Client.builder()
            .apiKey(apiKey)
            .build();
    }
}
