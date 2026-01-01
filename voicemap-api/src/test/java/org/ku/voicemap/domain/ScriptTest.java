package org.ku.voicemap.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.ku.voicemap.domain.script.Script;

class ScriptTest {

    private static final String CHAT_ID = "test-chat-id";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    void 이미_답변된_문답에_다시_답변할_수_없다() {
        Script script = new Script(CHAT_ID, "Question", NOW);
        script.answer("Answer", NOW);
        assertThatThrownBy(() -> script.answer("Another Answer", NOW))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", ""})
    void 질문이_비어있다면_예외를_발생한다(String question) {
        assertThatThrownBy(() -> new Script(CHAT_ID, question, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 문답을_생성한다() {
        assertDoesNotThrow(() -> new Script(CHAT_ID, "Question", NOW));
    }

    @Test
    void 문답에_답변한다() {
        Script script = new Script(CHAT_ID, "Question", NOW);
        script.answer("Answer", NOW);
        assertThat(script.isAnswered()).isTrue();
        assertThat(script.getAnswer()).isEqualTo("Answer");
    }
}
