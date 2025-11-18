package org.ku.voicemap.domain.auth.service;

import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;

public interface ExternalMemberInfoProvider {
    boolean supports(AuthProvider provider);
    ExternalMember provide(String providerToken);
}
