package org.ku.voicemap.domain.member.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "member", uniqueConstraints = {
    @UniqueConstraint(
        name = "UK_member_provider",
        columnNames = {"provider_id", "email", "provider"}
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "provider_id", nullable = false, unique = true, length = 50)
    private String providerId;
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @Builder
    private Member(String providerId, String email, Provider provider) {
        this.providerId = providerId;
        this.email = email;
        this.provider = provider;
        this.createdAt = LocalDateTime.now();
    }

    public static Member createMember(String providerId, String email, Provider provider) {
        return Member.builder()
            .providerId(providerId)
            .email(email)
            .provider(provider)
            .build();
    }


}
