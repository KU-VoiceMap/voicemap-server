package org.ku.voicemap.domain.llm.dto;

import java.util.List;

public record ScriptSummaryDto(String summary, List<String> keywords) {
}
