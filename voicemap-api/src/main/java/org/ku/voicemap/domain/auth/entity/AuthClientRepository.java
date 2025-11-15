package org.ku.voicemap.domain.auth.entity;

import java.util.Optional;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthClientRepository extends JpaRepository<AuthClient, Long> {

    Optional<AuthClient> findByProviderAndPrincipal(AuthProvider provider, String principal);

    Optional<AuthClient> findByMemberNumber(String memberNumber);
}
