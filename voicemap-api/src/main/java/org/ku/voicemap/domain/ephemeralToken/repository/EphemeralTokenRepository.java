package org.ku.voicemap.domain.ephemeralToken.repository;

import java.util.Optional;
import java.util.UUID;
import org.ku.voicemap.domain.ephemeralToken.entity.EphemeralToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EphemeralTokenRepository extends JpaRepository<EphemeralToken, UUID> {

    Optional<EphemeralToken> findByUserIdAndEphemeralToken(Long userId, String token);
}
