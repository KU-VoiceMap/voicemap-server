package org.ku.voicemap.ai.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdeaContextResultTest {

    @Test
    void testConstruction() {
        String coreIdea = "Core idea";
        String decisions = "Decisions made";
        String currentPhase = "Phase 1";
        String unexplored = "Unexplored topics";

        IdeaContextResult result = new IdeaContextResult(coreIdea, decisions, currentPhase, unexplored);

        assertEquals(coreIdea, result.coreIdea());
        assertEquals(decisions, result.decisions());
        assertEquals(currentPhase, result.currentPhase());
        assertEquals(unexplored, result.unexplored());
    }

    @Test
    void testAllAccessors() {
        IdeaContextResult result = new IdeaContextResult("idea", "decisions", "phase", "unexplored");

        assertNotNull(result.coreIdea());
        assertNotNull(result.decisions());
        assertNotNull(result.currentPhase());
        assertNotNull(result.unexplored());
    }

    @Test
    void testEquality() {
        IdeaContextResult result1 = new IdeaContextResult("idea", "decisions", "phase", "unexplored");
        IdeaContextResult result2 = new IdeaContextResult("idea", "decisions", "phase", "unexplored");
        assertEquals(result1, result2);
    }

    @Test
    void testInequality() {
        IdeaContextResult result1 = new IdeaContextResult("idea1", "decisions", "phase", "unexplored");
        IdeaContextResult result2 = new IdeaContextResult("idea2", "decisions", "phase", "unexplored");
        assertNotEquals(result1, result2);
    }

    @Test
    void testNullFields() {
        IdeaContextResult result = new IdeaContextResult(null, null, null, null);
        assertNull(result.coreIdea());
        assertNull(result.decisions());
        assertNull(result.currentPhase());
        assertNull(result.unexplored());
    }
}
