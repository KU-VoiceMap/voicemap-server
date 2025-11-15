package org.ku.voicemap.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Token {

    @Id
    @Column(updatable = false, length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_client_id", nullable = false)
    private AuthClient authClient;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    private String accessToken;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(1000)")
    private String refreshToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private Boolean reissuable;

    public Token(String accessToken, String refreshToken, LocalDateTime createdAt, LocalDateTime expireAt) {
        this.id = UUID.randomUUID();
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expireAt = expireAt;
        this.createdAt = createdAt;
        this.reissuable = true;
    }

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void invalidate() {
        this.reissuable = false;
    }

    public boolean isValid(LocalDateTime now) {
        return (now.equals(expireAt) || now.isBefore(expireAt)) && reissuable;
    }
}
