package org.ku.voicemap.domain.member;

import jakarta.validation.Valid;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.dto.RegisterDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/register")
public class MemberRegistrationController {

    /**
     * SNS 로그인 시도 실패 시 프론트에서 redirect한다. <br>
     * 인증 정보를 기반으로 회원 정보와 연동한다.
     */
    @PostMapping
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterDto registerInfo) {
        return null;
    }
}
