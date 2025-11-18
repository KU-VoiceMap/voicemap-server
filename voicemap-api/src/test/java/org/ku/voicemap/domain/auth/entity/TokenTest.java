package org.ku.voicemap.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;

class TokenTest {

    @Test
    void accessToken을_업데이트한다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        LocalDateTime issuedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expireAt = issuedAt.plusMonths(1);
        Token token = new Token(client, "access", "refresh", issuedAt, expireAt);
        token.updateAccessToken("newAccess");
        assertThat(token.getAccessToken()).isEqualTo("newAccess");
    }

    @Test
    void 유효한_토큰인지_확인한다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        LocalDateTime issuedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expireAt = issuedAt.plusMonths(1);
        Token token = new Token(client, "access", "refresh", issuedAt, expireAt);
        assertThat(token.isValid(issuedAt.plusDays(10))).isTrue();
        assertThat(token.isValid(expireAt.plusDays(10))).isFalse();
    }

    @Test
    void 무효화된_토큰은_기간_안에도_유효하지_않다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        LocalDateTime issuedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime expireAt = issuedAt.plusMonths(1);
        Token token = new Token(client, "access", "refresh", issuedAt, expireAt);
        token.invalidate();
        assertThat(token.isValid(issuedAt.plusDays(10))).isFalse();
    }
}
