package org.ku.voicemap.domain.auth;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.dto.TokenRequest;
import org.ku.voicemap.domain.auth.service.AuthClientNotConnectedException;
import org.ku.voicemap.domain.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * SNS 로그인 <p> 인증 정보를 저장하며, 회원 정보가 존재한다면 토큰까지 반환한다. <p> 연동된 회원이 존재하지 않으면 예외를 반환한다.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody TokenRequest request) {
        try {
            TokenResponse response = authService.login(request.email(), request.provider(), request.principal());
            return ResponseEntity.ok(response);
        } catch (AuthClientNotConnectedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody String refreshToken) {
        authService.logout(refreshToken);
    }

    @PostMapping("/access")
    public ResponseEntity<TokenResponse> rotateAccessToken(@RequestBody String refreshToken) {
        TokenResponse response = authService.rotateAccessToken(refreshToken, LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> rotateRefreshToken(@RequestBody String refreshToken) {
        TokenResponse response = authService.rotateRefreshToken(refreshToken, LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
