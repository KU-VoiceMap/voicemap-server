package org.ku.voicemap.domain.auth.service.provider.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.service.ExternalMemberInfoProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(GoogleProperties.class)
public class GoogleExternalMemberInfoProvider implements ExternalMemberInfoProvider {

    private final GoogleIdTokenVerifier idTokenVerifier;

    public GoogleExternalMemberInfoProvider(GoogleProperties properties) {
        this.idTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(properties.clientId()))
            .build();
    }

    @Override
    public boolean supports(AuthProvider provider) {
        return provider == AuthProvider.GOOGLE;
    }

    @Override
    public ExternalMember provide(String providerToken) {
        try {
            GoogleIdToken idToken = idTokenVerifier.verify(providerToken);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token.");
            }
            Payload payload = idToken.getPayload();
            return new ExternalMember(AuthProvider.GOOGLE, payload.getEmail(), payload.getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to verify Google ID token.", e);
        }
    }
}
