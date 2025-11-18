package org.ku.voicemap.domain.auth.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalMemberInfoResolver {

    private final List<ExternalMemberInfoProvider> providers;

    public ExternalMember resolve(AuthProvider provider, String providerToken) {
        ExternalMemberInfoProvider externalMemberInfoProvider = providers.stream()
            .filter(p -> p.supports(provider))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 인증 제공자입니다."));
        return externalMemberInfoProvider.provide(providerToken);
    }
}
