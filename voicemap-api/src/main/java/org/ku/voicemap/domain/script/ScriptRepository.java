package org.ku.voicemap.domain.script;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {

    List<Script> findAllByChatId(String chatId);
}
