package org.ku.voicemap.domain.script;

import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.script.dto.ScriptGetPagination;
import org.ku.voicemap.domain.script.service.ScriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scripts")
public class ScriptController {

    private final ScriptService scriptService;

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<ScriptGetPagination> getScriptsPagination(@PathVariable UUID chatId,
                                                                    @RequestParam(required = false) Long lastId,
                                                                    @RequestParam(defaultValue = "10") @Min(1) int size) {
        return ResponseEntity.ok(scriptService.getScriptByChatIdPagination(chatId, lastId, size));
    }
}
