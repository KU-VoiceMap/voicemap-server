package org.ku.voicemap.domain.auth.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import org.ku.voicemap.domain.auth.service.ExternalMemberInfoProvider;
import org.ku.voicemap.domain.auth.service.provider.google.GoogleExternalMemberInfoProvider;
import org.ku.voicemap.domain.auth.service.provider.google.GoogleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleProperties.class)
public class AuthProviderConfig {

    @Bean
    public ExternalMemberInfoProvider googleExternalMemberInfoProvider(GoogleProperties properties) {
        GoogleIdTokenVerifier idTokenVerifier = new Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(properties.clientId()))
            .build();
        return new GoogleExternalMemberInfoProvider(idTokenVerifier);
    }
}
