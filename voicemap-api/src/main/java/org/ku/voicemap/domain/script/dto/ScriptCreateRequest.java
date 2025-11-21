package org.ku.voicemap.domain.script.dto;

import java.util.UUID;

public record ScriptCreateRequest(
    UUID chatId,
    String question
) {

}
