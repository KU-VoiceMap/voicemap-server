package org.ku.voicemap.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScriptTest {

    @Test
    void 이미_답변된_문답에_다시_답변할_수_없다() {
        Script script = new Script("Question");
        script.answer("Answer");
        assertThatThrownBy(() -> script.answer("Another Answer"))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", ""})
    void 질문이_비어있다면_예외를_발생한다(String question) {
        assertThatThrownBy(() -> new Script(question))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 문답을_생성한다() {
        assertDoesNotThrow(() -> new Script("Question"));
    }

    @Test
    void 문답에_답변한다() {
        Script script = new Script("Question");
        script.answer("Answer");
        assertThat(script.isAnswered()).isTrue();
        assertThat(script.getAnswer()).isEqualTo("Answer");
    }
}
