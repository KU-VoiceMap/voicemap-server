package org.ku.voicemap.domain.LLM.service;

import org.ku.voicemap.domain.LLM.dto.ScriptSummaryDto;
import org.ku.voicemap.domain.LLM.dto.ScriptToLLMDto;

public interface LlmService {
    ScriptSummaryDto summaryScript(ScriptToLLMDto scripts);
}
