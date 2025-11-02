package org.ku.voicemap.domain.member.repository;

import java.util.Optional;
import org.ku.voicemap.domain.member.entity.Member;
import org.ku.voicemap.domain.member.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderIdAndProvider(String providerId,
                                                         Provider provider);
}
