package org.ku.voicemap.domain.llm.service;

import org.ku.voicemap.domain.llm.dto.ScriptSummaryDto;
import org.ku.voicemap.domain.llm.dto.ScriptToLLMDto;

public interface LlmService {
    ScriptSummaryDto summaryScript(ScriptToLLMDto scripts);
}
