package org.ku.voicemap.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "chat",
    indexes = {
        @Index(name = "idx_chat_room_member_id", columnList = "member_id")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    private static final int TITLE_MAX_LENGTH = 100;

    @Id
    private String id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", length = TITLE_MAX_LENGTH, nullable = false)
    private String title;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Script> scripts;

    public Chat(long memberId, String title) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("채팅 제목은 필수 입력 사항입니다.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("채팅 제목의 길이를 초과합니다.");
        }
        this.id = UUID.randomUUID().toString();
        this.memberId = memberId;
        this.title = title;
    }
}
