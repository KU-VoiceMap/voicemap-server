package org.ku.voicemap.domain.member;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.service.ExternalMemberInfoResolver;
import org.ku.voicemap.domain.member.dto.MemberRegisterRequest;
import org.ku.voicemap.domain.member.dto.MemberRegisterResponse;
import org.ku.voicemap.domain.member.service.MemberRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/register")
@RequiredArgsConstructor
public class MemberRegistrationController {

    private final ExternalMemberInfoResolver externalMemberInfoResolver;
    private final MemberRegistrationService memberRegistrationService;

    /**
     * SNS 로그인 시도 실패 시 프론트에서 redirect한다. <br> 인증 정보를 기반으로 회원 정보와 연동한다.
     */
    @PostMapping
    public ResponseEntity<MemberRegisterResponse> register(@Valid @RequestBody MemberRegisterRequest request) {
        // TODO: 세션을 도입하여 두 번 호출하지 않도록 개선한다.
        ExternalMember externalMember = externalMemberInfoResolver.resolve(request.provider(), request.providerToken());
        return ResponseEntity.ok(memberRegistrationService.register(externalMember, request.email()));
    }
}
