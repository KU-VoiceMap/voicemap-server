package org.ku.voicemap.domain.auth.entity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByRefreshToken(String refreshToken);

    @Query("SELECT t FROM Token t JOIN FETCH t.authClient WHERE t.refreshToken = :refreshToken")
    Optional<Token> findByRefreshTokenWithAuthClient(String refreshToken);
}
