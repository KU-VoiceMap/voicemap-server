package org.ku.voicemap.domain.jwt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(updatable = false, length = 36)
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    private String accessToken;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    private String refreshToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private boolean possible = true;


    public RefreshToken(Long member, String accessToken, String refreshToken, LocalDateTime createdAt,
                        LocalDateTime expireAt) {
        this.id = UUID.randomUUID();
        this.memberId = member;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expireAt = expireAt;
        this.createdAt = createdAt;
    }

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void updatePossible() {
        this.possible = false;
    }

}