package org.ku.voicemap.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ku.voicemap.domain.auth.AuthProvider;

// TODO: Session 도입 후 sessionId로 수정
public record MemberRegisterRequest(
    @NotNull AuthProvider provider,
    @NotBlank String providerToken,
    @NotBlank String email
) {
}
