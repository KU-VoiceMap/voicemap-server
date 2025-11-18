package org.ku.voicemap.domain.ephemeralToken;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.ephemeralToken.dto.CreateEphemeralRequest;
import org.ku.voicemap.domain.ephemeralToken.dto.EphemeralTokenResponse;
import org.ku.voicemap.domain.ephemeralToken.service.EphemeralService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/token")
public class EphemeralTokenController {

    private final EphemeralService ephemeralService;

    @GetMapping
    public ResponseEntity<EphemeralTokenResponse> rotateToken(@RequestBody Long userId, @RequestBody String oldToken) {
        CreateEphemeralRequest createEphemeralRequest = new CreateEphemeralRequest(10, 5, 5);
        return ResponseEntity.ok(ephemeralService.rotateEphemeralToken(userId, oldToken, createEphemeralRequest));
    }

}
