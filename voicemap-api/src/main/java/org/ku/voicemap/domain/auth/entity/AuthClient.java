package org.ku.voicemap.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ku.voicemap.domain.auth.AuthProvider;

@Table(
    name = "auth_client",
    indexes = {
        @Index(name = "uk_auth_client_provider_principal", columnList = "provider, principal", unique = true),
        @Index(name = "idx_auth_client_member_number", columnList = "member_number"),
    }
)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthClient {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "member_number", nullable = true)
    private String memberNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider;

    @Column(name = "principal", nullable = false)
    private String principal;

    @Column(name = "is_connected", nullable = false)
    private Boolean isConnected;

    public AuthClient(String email, AuthProvider provider, String principal) {
        this.provider = provider;
        this.memberNumber = null;
        this.principal = principal;
        this.email = email;
        this.isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect(String memberNumber) {
        if (isConnected() && !this.memberNumber.equals(memberNumber)) {
            throw new IllegalStateException("이미 다른 회원에 연결되었습니다.");
        }
        this.memberNumber = memberNumber;
        this.isConnected = true;
    }
}
