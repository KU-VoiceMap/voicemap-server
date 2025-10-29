package org.ku.voicemap.domain.member.entity;

import java.time.LocalDateTime;

public record MemberDto(Long id, String providerId, String email
    , LocalDateTime createdAt, Provider provider) {

  public static MemberDto toDto(Member member) {
    return new MemberDto(member.getId(), member.getProviderId(), member.getEmail()
        , member.getCreatedAt(), member.getProvider());
  }

}
