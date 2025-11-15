package org.ku.voicemap.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.service.AuthConnectService;
import org.ku.voicemap.domain.member.dto.MemberRegisterResponse;
import org.ku.voicemap.domain.member.entity.Member;
import org.ku.voicemap.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final MemberRepository memberRepository;
    private final AuthConnectService authConnectService;

    @Transactional
    public MemberRegisterResponse register(ExternalMember externalMember, String email) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> memberRepository.save(new Member(email)));
        TokenResponse tokenResponse = authConnectService.connect(externalMember, member.getMemberNumber());
        return new MemberRegisterResponse(member.getMemberNumber(), tokenResponse.accessToken(), tokenResponse.refreshToken());
    }
}
