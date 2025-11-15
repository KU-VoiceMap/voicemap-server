package org.ku.voicemap.domain.member.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.entity.Member;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.repository.MemberRepository;
import org.ku.voicemap.domain.auth.dto.RegisterDto;
import org.ku.voicemap.exception.member.MemberExistRegister;
import org.ku.voicemap.exception.member.MemberNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberDto createMember(RegisterDto registerInfo) {

        Member member = Member.createMember(registerInfo.providerId(), registerInfo.email(),
            registerInfo.provider());

        try {

            memberRepository.saveAndFlush(member);

        } catch (DataIntegrityViolationException e) {

            throw new MemberExistRegister(registerInfo);

        }

        return MemberDto.toDto(member);
    }


    @Transactional(readOnly = true)
    public MemberDto findMember(RegisterDto registerInfo) {

        Optional<Member> member = memberRepository.findByProviderIdAndProvider(
            registerInfo.providerId(), registerInfo.provider());

        return member.map(MemberDto::toDto)
            .orElseThrow(() -> new MemberNotFoundException(registerInfo));
    }

    @Transactional(readOnly = true)
    public MemberDto findMember(Long memberId) {

        Optional<Member> member = memberRepository.findById(
            memberId);

        return member.map(MemberDto::toDto)
            .orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}
