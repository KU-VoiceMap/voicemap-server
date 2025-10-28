package org.ku.voicemap.domain.oauth.service;

import org.ku.voicemap.domain.member.entity.Provider;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;

public interface AuthServiceInter {
  public AuthResponse register(Provider provider, String idToken);
  public AuthResponse login(Provider provider, String idToken);
}
