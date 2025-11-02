package org.ku.voicemap;

import org.ku.voicemap.config.GoogleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GoogleProperties.class)
public class VoiceMapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceMapApiApplication.class, args);
    }
}
