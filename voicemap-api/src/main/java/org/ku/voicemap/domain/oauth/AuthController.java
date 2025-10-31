package org.ku.voicemap.domain.oauth;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.model.Provider;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;
import org.ku.voicemap.domain.oauth.dto.TokenRequest;
import org.ku.voicemap.domain.oauth.service.AuthServiceInter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceInter authService;

    @PostMapping("/{provider}/register")
    public ResponseEntity<AuthResponse> register(
        @PathVariable String provider,
        @RequestBody TokenRequest request) {

        if (Provider.checkProvider(provider)) {
            throw new RuntimeException("잘못된 provider");
        }
        Provider providerEnum = Provider.valueOf(provider.toUpperCase());

        AuthResponse response = authService.register(providerEnum, request.idToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/{provider}/login")
    public ResponseEntity<AuthResponse> login(
        @PathVariable String provider,
        @RequestBody TokenRequest request) {

        if (Provider.checkProvider(provider)) {
            throw new RuntimeException("잘못된 provider");
        }

        Provider providerEnum = Provider.valueOf(provider.toUpperCase());

        AuthResponse response = authService.login(providerEnum, request.idToken());

        return ResponseEntity.ok(response);
    }

}