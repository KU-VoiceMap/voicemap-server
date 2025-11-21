package org.ku.voicemap.domain.script.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.script.dto.ScriptCreateResponse;
import org.ku.voicemap.domain.script.dto.ScriptDto;
import org.ku.voicemap.domain.script.dto.ScriptGetPagination;
import org.ku.voicemap.domain.script.dto.ScriptGetResponse;
import org.ku.voicemap.domain.script.entity.Script;
import org.ku.voicemap.domain.script.repository.ScriptRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptRepository scriptRepository;

    @Transactional
    public ScriptCreateResponse createScript(UUID chatId, String question, LocalDateTime createdAt) {
        Script script = new Script(chatId, question, createdAt);
        scriptRepository.save(script);
        return new ScriptCreateResponse(script.getId(), script.isAnswered(),
            script.getCreatedAt(), script.getAnsweredAt());
    }

    @Transactional
    public ScriptCreateResponse answerScript(Long scriptId, String answer, LocalDateTime answeredAt) {
        Script script = scriptRepository.findById(scriptId).
            orElseThrow(IllegalArgumentException::new);
        script.answer(answer, answeredAt);
        return new ScriptCreateResponse(script.getId(), script.isAnswered(),
            script.getCreatedAt(), script.getAnsweredAt());
    }

    @Transactional(readOnly = true)
    public ScriptGetResponse getScriptByChatId(UUID chatId) {
        List<Script> scripts = scriptRepository.findAllByChatIdOrderByCreatedAtAsc(chatId);
        List<ScriptDto> scriptDtos = scripts.stream()
            .map(ScriptDto::toDto)
            .toList();
        return new ScriptGetResponse(scriptDtos);
    }

    @Transactional(readOnly = true)
    public ScriptGetPagination getScriptByChatIdPagination(UUID chatId, LocalDateTime lastCreatedAt, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Script> scripts;
        if (lastCreatedAt == null) {
            scripts = scriptRepository.findAllByChatIdOrderByCreatedAtDesc(chatId, pageable);
        } else {
            scripts = scriptRepository.findByChatIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                chatId, lastCreatedAt, pageable);
        }
        boolean isNext = false;
        if (scripts.size() > size) {
            isNext = true;
            scripts.remove(size);
        }
        List<ScriptDto> scriptDtos = scripts.stream()
            .map(ScriptDto::toDto)
            .toList();
        LocalDateTime nextCursor = null;
        if (!scriptDtos.isEmpty()) {
            nextCursor = scriptDtos.get(scriptDtos.size() - 1).createdAt();
        }
        return new ScriptGetPagination(scriptDtos, nextCursor, isNext);
    }

}
