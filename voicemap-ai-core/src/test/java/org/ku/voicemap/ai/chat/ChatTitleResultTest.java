package org.ku.voicemap.ai.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatTitleResultTest {

    @Test
    void testConstruction() {
        String title = "Test Title";
        ChatTitleResult result = new ChatTitleResult(title);
        assertEquals(title, result.title());
    }

    @Test
    void testNullTitle() {
        ChatTitleResult result = new ChatTitleResult(null);
        assertNull(result.title());
    }

    @Test
    void testEquality() {
        ChatTitleResult result1 = new ChatTitleResult("Title");
        ChatTitleResult result2 = new ChatTitleResult("Title");
        assertEquals(result1, result2);
    }

    @Test
    void testInequality() {
        ChatTitleResult result1 = new ChatTitleResult("Title 1");
        ChatTitleResult result2 = new ChatTitleResult("Title 2");
        assertNotEquals(result1, result2);
    }

    @Test
    void testToString() {
        ChatTitleResult result = new ChatTitleResult("Test Title");
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("Test Title"));
    }
}
