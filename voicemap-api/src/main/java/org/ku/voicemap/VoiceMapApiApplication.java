package org.ku.voicemap;

import org.ku.voicemap.config.GoogleProperties;
import org.ku.voicemap.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GoogleProperties.class,JwtProperties.class})
public class VoiceMapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceMapApiApplication.class, args);
    }
}
