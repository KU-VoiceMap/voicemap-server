package org.ku.voicemap.domain.script.repository;

import java.util.List;
import java.util.UUID;
import org.ku.voicemap.domain.script.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> , ScriptRepositoryCustomInterface{

    List<Script> findAllByChatIdOrderByCreatedAtAsc(UUID chatId);

}
