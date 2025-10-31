package org.ku.voicemap.domain.oauth.service;

import org.ku.voicemap.domain.member.model.Provider;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;

public interface AuthServiceInter {

    AuthResponse register(Provider provider, String idToken);

    AuthResponse login(Provider provider, String idToken);
}
