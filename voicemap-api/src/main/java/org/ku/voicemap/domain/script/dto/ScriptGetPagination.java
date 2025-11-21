package org.ku.voicemap.domain.script.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ScriptGetPagination(List<ScriptDto> scriptDtoList,
                                  LocalDateTime lastCreateAt,
                                  boolean isNext) {
}
