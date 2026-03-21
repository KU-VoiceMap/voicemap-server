package org.ku.voicemap.ai.realtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiRoleTest {

    @Test
    void hasUserAndAgentValues() {
        AiRole[] values = AiRole.values();

        assertEquals(2, values.length);
        assertEquals(AiRole.USER, AiRole.valueOf("USER"));
        assertEquals(AiRole.AGENT, AiRole.valueOf("AGENT"));
    }

    @Test
    void valueOfThrowsForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> AiRole.valueOf("SYSTEM"));
    }
}
