package org.ku.voicemap.domain.script.entity;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
    catalog = "voicemap",
    name = "script",
    indexes = {
        @Index(name = "idx_script_chat_id", columnList = "chat_id, id")
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
    private UUID chatId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = true, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Script(UUID chatId, String question, String answer) {
//        if (StringUtils.isBlank(question)) {
//            throw new IllegalArgumentException("질문은 필수 입력 사항입니다.");
//        }
        this.chatId = chatId;
        this.question = question;
        this.answer=answer;
        this.createdAt = LocalDateTime.now();
    }

}
