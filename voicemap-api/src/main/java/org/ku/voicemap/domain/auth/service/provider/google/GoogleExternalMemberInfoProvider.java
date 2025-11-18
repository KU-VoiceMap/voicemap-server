package org.ku.voicemap.domain.auth.service.provider.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.service.ExternalMemberInfoProvider;

@RequiredArgsConstructor
public class GoogleExternalMemberInfoProvider implements ExternalMemberInfoProvider {

    private final GoogleIdTokenVerifier idTokenVerifier;

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
