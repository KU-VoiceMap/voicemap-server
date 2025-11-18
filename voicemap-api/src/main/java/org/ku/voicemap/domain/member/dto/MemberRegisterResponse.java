package org.ku.voicemap.domain.member.dto;

public record MemberRegisterResponse(String memberNumber, String accessToken, String refreshToken) {
}
