package org.ku.voicemap.domain.script.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.ku.voicemap.domain.script.entity.Script;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {

    List<Script> findAllByChatIdOrderByCreatedAtAsc(UUID chatId);

    List<Script> findAllByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    List<Script> findByChatIdAndCreatedAtLessThanOrderByCreatedAtDesc(
        UUID chatId,
        LocalDateTime createdAt,
        Pageable pageable
    );

}
