package org.ku.voicemap.domain.LLM.dto;

import java.util.List;

public record ScriptSummaryDto(String summary, List<String> keywords) {
}
