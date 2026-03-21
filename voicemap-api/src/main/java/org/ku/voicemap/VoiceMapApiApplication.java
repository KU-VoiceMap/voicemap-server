package org.ku.voicemap;

import org.ku.voicemap.config.AiInstructionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiInstructionProperties.class)
public class VoiceMapApiApplication {

    static void main(String[] args) {
        SpringApplication.run(VoiceMapApiApplication.class, args);
    }
}
