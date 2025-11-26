package org.ku.voicemap.domain.script.dto;

import java.util.List;

public record ScriptGetPagination(List<ScriptDto> scriptDtoList,
                                  Long nextCursor,
                                  boolean isNext) {
}
