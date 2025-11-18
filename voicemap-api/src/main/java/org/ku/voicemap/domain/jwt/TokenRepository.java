package org.ku.voicemap.domain.jwt;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByRefreshToken(String refreshToken);
}
