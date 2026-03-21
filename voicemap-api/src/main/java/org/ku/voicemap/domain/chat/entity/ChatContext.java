package org.ku.voicemap.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "chat_context",
    indexes = {
        @Index(name = "idx_chat_context_chat_id", columnList = "chat_id")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, length = 36)
    private String chatId;

    @Lob
    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatContext(String chatId, String context) {
        if (StringUtils.isBlank(chatId)) {
            throw new IllegalArgumentException("채팅 ID는 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(context)) {
            throw new IllegalArgumentException("컨텍스트는 필수 입력 사항입니다.");
        }
        this.chatId = chatId;
        this.context = context;
        this.createdAt = LocalDateTime.now();
    }
}
