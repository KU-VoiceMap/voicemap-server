package org.ku.voicemap.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "chat",
    indexes = {
        @Index(name = "idx_chat_member_number", columnList = "member_number")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    private static final int TITLE_MAX_LENGTH = 100;

    @Id
    private String id;

    @Column(name = "member_number", nullable = false, length = 50)
    private String memberNumber;

    @Column(name = "title", length = TITLE_MAX_LENGTH, nullable = false)
    private String title;

    public Chat(String memberNumber, String title) {
        if (StringUtils.isBlank(memberNumber)) {
            throw new IllegalArgumentException("회원 번호는 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("채팅 제목은 필수 입력 사항입니다.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("채팅 제목의 길이를 초과합니다.");
        }
        this.id = UUID.randomUUID().toString();
        this.memberNumber = memberNumber;
        this.title = title;
    }
}
