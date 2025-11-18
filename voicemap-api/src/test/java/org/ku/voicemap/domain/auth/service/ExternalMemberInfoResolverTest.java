package org.ku.voicemap.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;

class ExternalMemberInfoResolverTest {

    static class DummyGoogleProvider implements ExternalMemberInfoProvider {
        @Override
        public boolean supports(AuthProvider provider) {
            return provider == AuthProvider.GOOGLE;
        }

        @Override
        public ExternalMember provide(String providerToken) {
            return new ExternalMember(AuthProvider.GOOGLE, "principal", "email");
        }
    }

    @Test
    void 지원하는_인증자가_없으면_예외를_발생한다() {
        ExternalMemberInfoResolver resolver = new ExternalMemberInfoResolver(List.of());
        assertThatThrownBy(() -> resolver.resolve(AuthProvider.GOOGLE, "token"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 지원하는_인증자에_대하여_외부인증을_제공한다() {
        ExternalMemberInfoResolver resolver = new ExternalMemberInfoResolver(List.of(new DummyGoogleProvider()));
        ExternalMember externalMember = resolver.resolve(AuthProvider.GOOGLE, "token");
        assertThat(externalMember)
            .usingRecursiveComparison()
            .isEqualTo(new ExternalMember(AuthProvider.GOOGLE, "principal", "email"));
    }
}
