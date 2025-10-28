package org.ku.voicemap.domain.member.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.entity.Member;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.repository.MemberRepository;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService implements MemberServiceInter{

  private final MemberRepository memberRepository;

  @Override
  public MemberDto createMember(RegisterDto registerInfo) {
    Member member=Member.createMember(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
    memberRepository.save(member);
    return MemberDto.toDto(member);
  }

  @Override
  public boolean checkRegister(RegisterDto registerInfo) {
    return memberRepository.existsByProviderIdAndEmailAndProvider(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
  }

  @Override
  public MemberDto findMember(RegisterDto registerInfo) {
    Optional<Member> member =memberRepository.findByProviderIdAndEmailAndProvider(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
    return member.map(MemberDto::toDto).orElse(null);
  }
}
