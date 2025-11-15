package org.ku.voicemap.domain.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.AuthResponse;
import org.ku.voicemap.domain.auth.dto.TokenRequest;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody TokenRequest request) {
        AuthResponse response = authService.register(request.provider(), request.idToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody TokenRequest request) {
        AuthResponse response = authService.login(request.provider(), request.idToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody String clientRefreshToken) {
        authService.logout(clientRefreshToken);
    }

    @PostMapping("/access")
    public ResponseEntity<AuthResponse> rotateAccessToken(@RequestBody String clientRefreshToken) {
        AuthResponse response = authService.rotateAccessToken(clientRefreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> rotateRefreshToken(@RequestBody String clientRefreshToken) {
        AuthResponse response = authService.rotateRefreshToken(clientRefreshToken);
        return ResponseEntity.ok(response);
    }
}
