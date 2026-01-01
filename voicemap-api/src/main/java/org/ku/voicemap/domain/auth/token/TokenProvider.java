package org.ku.voicemap.domain.auth.token;

import java.time.LocalDateTime;

public interface TokenProvider {

    TokenPair generateTokenPair(String memberNumber, LocalDateTime now);

    String generateAccessToken(String memberNumber, LocalDateTime now);

    boolean validateToken(String token);

    String extractMemberNumber(String token);
}
