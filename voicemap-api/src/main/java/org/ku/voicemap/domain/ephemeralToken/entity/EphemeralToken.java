package org.ku.voicemap.domain.ephemeralToken.entity;

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
@Getter
@Table(name = "ephemeral_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EphemeralToken {
    @Id
    @Column(nullable = false, updatable = false)
    UUID id;
    @Column(nullable = false, updatable = false)
    private Long userId;
    @Column(nullable = false, updatable = false)
    private int uses;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, updatable = false)
    private int expireMinutes;
    @Column(nullable = false, updatable = false)
    private int sessionExpireMinutes;
    @Column(nullable = false, updatable = false)
    private String ephemeralToken;

    public EphemeralToken(Long userId, int uses, int expireMinutes, int sessionExpireMinutes, String ephemeralToken) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.uses = uses;
        this.expireMinutes = expireMinutes;
        this.sessionExpireMinutes = sessionExpireMinutes;
        this.ephemeralToken = ephemeralToken;
        this.createdAt = LocalDateTime.now();
    }


}
