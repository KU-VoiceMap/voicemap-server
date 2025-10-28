package org.ku.voicemap.domain.member.service;

import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;

public interface MemberServiceInter {
  MemberDto createMember(RegisterDto registerInfo);
  boolean checkRegister(RegisterDto registerInfo);
  MemberDto findMember(RegisterDto registerInfo);
}
