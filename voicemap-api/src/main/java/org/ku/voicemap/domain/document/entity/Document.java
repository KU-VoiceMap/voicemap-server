package org.ku.voicemap.domain.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "document",
    indexes = {
        @Index(name = "idx_document_member_number", columnList = "member_number"),
        @Index(name = "idx_document_chat_id", columnList = "chat_id")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    private static final int TITLE_MAX_LENGTH = 200;

    @Id
    private String id;

    @Column(name = "chat_id", nullable = false, length = 36)
    private String chatId;

    @Column(name = "member_number", nullable = false, length = 50)
    private String memberNumber;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Lob
    @Column(name = "summary", nullable = false)
    private String summary;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Document(String chatId, String memberNumber, String title, String summary, String content, LocalDateTime createdAt) {
        if (StringUtils.isBlank(chatId)) {
            throw new IllegalArgumentException("채팅 ID는 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(memberNumber)) {
            throw new IllegalArgumentException("회원 번호는 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("문서 제목은 필수 입력 사항입니다.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("문서 제목의 길이를 초과합니다.");
        }
        if (StringUtils.isBlank(summary)) {
            throw new IllegalArgumentException("문서 요약은 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("문서 내용은 필수 입력 사항입니다.");
        }
        this.id = UUID.randomUUID().toString();
        this.chatId = chatId;
        this.memberNumber = memberNumber;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.createdAt = createdAt;
    }
}
