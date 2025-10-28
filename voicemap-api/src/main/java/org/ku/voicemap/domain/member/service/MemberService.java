package org.ku.voicemap.domain.member.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.entity.Member;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.repository.MemberRepository;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService implements MemberServiceInter{

  private final MemberRepository memberRepository;

  @Override
  @Transactional
  public MemberDto createMember(RegisterDto registerInfo) {
    Member member=Member.createMember(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
    memberRepository.save(member);
    return MemberDto.toDto(member);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean checkRegister(RegisterDto registerInfo) {
    return memberRepository.existsByProviderIdAndEmailAndProvider(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
  }

  @Override
  @Transactional(readOnly = true)
  public MemberDto findMember(RegisterDto registerInfo) {
    Optional<Member> member =memberRepository.findByProviderIdAndEmailAndProvider(registerInfo.providerId()
        , registerInfo.email(), registerInfo.provider());
    return member.map(MemberDto::toDto).orElse(null);
  }
}
