package org.ku.voicemap.domain.script.dto;

import java.util.List;

public record ScriptGetResponse(
    List<ScriptDto> scriptDtoList
) {
}
