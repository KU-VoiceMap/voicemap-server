package org.ku.voicemap.domain.auth.dto;

import org.ku.voicemap.domain.auth.OAuthProvider;

public record TokenRequest(OAuthProvider provider, String idToken) {

}
