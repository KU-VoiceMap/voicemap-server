package org.ku.voicemap;

import org.ku.voicemap.domain.auth.config.JwtProperties;
import org.ku.voicemap.domain.auth.service.provider.google.GoogleProperties;
import org.ku.voicemap.domain.ephemeralToken.config.EphemeralProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GoogleProperties.class, JwtProperties.class, EphemeralProperties.class})
public class VoiceMapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceMapApiApplication.class, args);
    }
}
