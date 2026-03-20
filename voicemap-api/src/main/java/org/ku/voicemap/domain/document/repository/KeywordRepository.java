package org.ku.voicemap.domain.document.repository;

import java.util.Optional;
import org.ku.voicemap.domain.document.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    @Transactional(readOnly = true)
    Optional<Keyword> findByName(String name);
}
