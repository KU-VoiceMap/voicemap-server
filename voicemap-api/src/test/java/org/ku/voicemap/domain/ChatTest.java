package org.ku.voicemap.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChatTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", ""})
    void 채팅_제목이_존재하지_않으면_예외를_발생한다(String title) {
        assertThatThrownBy(() -> new Chat(1L, title))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 채팅_제목이_100자를_초과하면_예외를_발생한다() {
        String title = "a".repeat(101);
        assertThatThrownBy(() -> new Chat(1L, title))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 채팅을_생성한다() {
        String title = "a".repeat(100);
        assertDoesNotThrow(() -> new Chat(1L, title));
    }
}
