package org.ku.voicemap.ai.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentResultTest {

    @Test
    void testConstruction() {
        String title = "Document Title";
        String summary = "Summary";
        String content = "Content";
        List<String> keywords = List.of("key1", "key2");

        DocumentResult result = new DocumentResult(title, summary, content, keywords);

        assertEquals(title, result.title());
        assertEquals(summary, result.summary());
        assertEquals(content, result.content());
        assertEquals(keywords, result.keywords());
    }

    @Test
    void testAllAccessors() {
        DocumentResult result = new DocumentResult("title", "summary", "content", List.of("k1", "k2", "k3"));

        assertNotNull(result.title());
        assertNotNull(result.summary());
        assertNotNull(result.content());
        assertNotNull(result.keywords());
        assertEquals(3, result.keywords().size());
    }

    @Test
    void testKeywordsImmutability() {
        List<String> keywords = List.of("key1", "key2");
        DocumentResult result = new DocumentResult("title", "summary", "content", keywords);

        assertThrows(UnsupportedOperationException.class, () -> result.keywords().add("key3"));
    }

    @Test
    void testEquality() {
        DocumentResult result1 = new DocumentResult("title", "summary", "content", List.of("k1", "k2"));
        DocumentResult result2 = new DocumentResult("title", "summary", "content", List.of("k1", "k2"));
        assertEquals(result1, result2);
    }

    @Test
    void testInequality() {
        DocumentResult result1 = new DocumentResult("title1", "summary", "content", List.of("k1"));
        DocumentResult result2 = new DocumentResult("title2", "summary", "content", List.of("k1"));
        assertNotEquals(result1, result2);
    }

    @Test
    void testEmptyKeywordsList() {
        DocumentResult result = new DocumentResult("title", "summary", "content", List.of());
        assertTrue(result.keywords().isEmpty());
    }

    @Test
    void testNullFields() {
        DocumentResult result = new DocumentResult(null, null, null, null);
        assertNull(result.title());
        assertNull(result.summary());
        assertNull(result.content());
        assertNull(result.keywords());
    }
}
