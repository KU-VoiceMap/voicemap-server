package org.ku.voicemap.ai.chat;

public record IdeaContextResult(
    String coreIdea,
    String decisions,
    String currentPhase,
    String unexplored
) {
}
