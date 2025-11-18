package org.ku.voicemap.domain.script;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
    catalog = "voicemap",
    name = "script",
    indexes = {
        @Index(name = "idx_script_chat_id", columnList = "chat_id")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = true, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "is_answered", nullable = false)
    private boolean isAnswered = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at", nullable = true)
    private LocalDateTime answeredAt;

    public Script(String question, LocalDateTime createdAt) {
        if (StringUtils.isBlank(question)) {
            throw new IllegalArgumentException("질문은 필수 입력 사항입니다.");
        }
        this.question = question;
        this.createdAt = createdAt;
    }

    public void answer(String answer, LocalDateTime answeredAt) {
        if (isAnswered) {
            throw new IllegalStateException("이미 완성된 문답입니다.");
        }
        this.answer = answer;
        this.isAnswered = true;
        this.answeredAt = answeredAt;
    }
}
