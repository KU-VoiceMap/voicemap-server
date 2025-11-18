package org.ku.voicemap.domain.auth.service.provider.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.mockito.BDDMockito;

class GoogleExternalMemberInfoProviderTest {

    private final GoogleIdTokenVerifier idTokenVerifier = mock(GoogleIdTokenVerifier.class);
    private final GoogleIdToken idToken = mock(GoogleIdToken.class);

    private GoogleExternalMemberInfoProvider provider;

    @BeforeEach
    void setUp() {
        BDDMockito.reset(idTokenVerifier, idToken);
        provider = new GoogleExternalMemberInfoProvider(idTokenVerifier);
    }

    @Test
    void supports() {
        assertThat(provider.supports(AuthProvider.GOOGLE)).isTrue();
    }

    @Test
    void 토큰검증에_실패하는_경우_예외를_발생한다() throws Exception {
        given(idTokenVerifier.verify(anyString())).willReturn(null);
        assertThatThrownBy(() -> provider.provide("token"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 외부_값을_가져오는데_성공한다() throws Exception {
        given(idTokenVerifier.verify(anyString())).willReturn(idToken);
        Payload payload = new Payload();
        payload.setEmail("test@email.com");
        payload.setSubject("principal");
        given(idToken.getPayload()).willReturn(payload);

        ExternalMember actual = provider.provide("token");
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(new ExternalMember(AuthProvider.GOOGLE, "test@email.com", "principal"));
    }
}
