package org.ku.voicemap.domain.member.dto;

import org.ku.voicemap.domain.auth.AuthProvider;

// TODO: Session 도입 후 sessionId로 수정
public record MemberRegisterRequest(AuthProvider provider, String providerToken, String email) {
}
