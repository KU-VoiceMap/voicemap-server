package org.ku.voicemap.domain.script.repository;

import java.util.List;
import java.util.UUID;
import org.ku.voicemap.domain.script.entity.Script;

public interface ScriptRepositoryCustomInterface {
    List<Script> findScriptsPagination(UUID chatId, Long lastId, int Size);
}
