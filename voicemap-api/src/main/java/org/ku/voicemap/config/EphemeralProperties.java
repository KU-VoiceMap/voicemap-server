package org.ku.voicemap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "python")
public class EphemeralProperties {

    private String apikey;
    private Script script = new Script();

    @Getter
    @Setter
    public static class Script {
        private Resource resourcePath;
    }
}
