package org.ku.voicemap.domain.member.dto;

public record MemberRegisterResponse(String email, String accessToken, String refreshToken) {
}
