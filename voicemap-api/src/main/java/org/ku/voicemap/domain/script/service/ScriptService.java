package org.ku.voicemap.domain.script.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.script.dto.ScriptDto;
import org.ku.voicemap.domain.script.dto.ScriptGetPagination;
import org.ku.voicemap.domain.script.entity.Script;
import org.ku.voicemap.domain.script.repository.ScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptRepository scriptRepository;

    @Transactional
    public ScriptDto createScript(UUID chatId, String question, String answer) {
        Script script = new Script(chatId, question, answer);
        scriptRepository.save(script);
        return ScriptDto.toDto(script);
    }

    @Transactional(readOnly = true)
    public String getScriptByChatId(UUID chatId) {
        List<Script> scripts = scriptRepository.findAllByChatIdOrderByCreatedAtAsc(chatId);
        if (scripts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Script script : scripts) {
            sb.append("question: ").append(script.getQuestion()).append("\n");
            if (script.getAnswer() != null && !script.getAnswer().isBlank()) {
                sb.append("answer: ").append(script.getAnswer()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public ScriptGetPagination getScriptByChatIdPagination(UUID chatId, Long lastId, int size) {
        int fetchSize = size + 1;

        List<Script> scripts = scriptRepository.findScriptsPagination(chatId, lastId, fetchSize);

        boolean isNext = false;
        if (scripts.size() > size) {
            isNext = true;
            scripts.remove(size);
        }

        List<ScriptDto> scriptDtos = scripts.stream()
            .map(ScriptDto::toDto)
            .toList();

        Long nextCursor = null;
        if (!scriptDtos.isEmpty()) {
            nextCursor = scriptDtos.get(scriptDtos.size() - 1).id();
        }

        return new ScriptGetPagination(scriptDtos, nextCursor, isNext);
    }

}
