package org.ku.voicemap.domain.script;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.script.dto.ScriptAnswerRequest;
import org.ku.voicemap.domain.script.dto.ScriptCreateRequest;
import org.ku.voicemap.domain.script.dto.ScriptCreateResponse;
import org.ku.voicemap.domain.script.dto.ScriptGetPagination;
import org.ku.voicemap.domain.script.service.ScriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scripts")
public class ScriptController {

    private final ScriptService scriptService;

    @PostMapping
    public ResponseEntity<ScriptCreateResponse> createScript(@RequestBody ScriptCreateRequest request) {
        ScriptCreateResponse response = scriptService.createScript(
            request.chatId(),
            request.question(),
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{scriptId}/answer")
    public ResponseEntity<ScriptCreateResponse> answerScript(@PathVariable Long scriptId,
                                                             @RequestBody ScriptAnswerRequest request) {
        ScriptCreateResponse response = scriptService.answerScript(
            scriptId,
            request.answer(),
            LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<ScriptGetPagination> getScriptsByChatIdPagination(@PathVariable UUID chatId,
                                                                            @RequestParam(required = false) LocalDateTime lastCreatedAt,
                                                                            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            scriptService.getScriptByChatIdPagination(chatId, lastCreatedAt, size)
        );
    }
}
