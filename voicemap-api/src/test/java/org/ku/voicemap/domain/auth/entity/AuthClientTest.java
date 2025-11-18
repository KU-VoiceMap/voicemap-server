package org.ku.voicemap.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;

class AuthClientTest {

    @Test
    void 연결_여부를_확인한다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        assertThat(client.isConnected()).isFalse();
    }

    @Test
    void 연결_후_회원번호를_수정하고_연결_여부를_활성화한다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        client.connect("memberNumber");
        assertThat(client.isConnected()).isTrue();
        assertThat(client.getMemberNumber()).isEqualTo("memberNumber");
    }

    @Test
    void 하나의_클라이언트에_다른_회원번호가_연결될_수_없다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        client.connect("memberNumber");
        assertThatThrownBy(() -> client.connect("test"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 하나의_클라이언트에_같은_회원번호_연결은_멱등성을_보장한다() {
        AuthClient client = new AuthClient("email", AuthProvider.GOOGLE, "principal");
        client.connect("memberNumber");
        assertThatCode(() -> client.connect("memberNumber"))
            .doesNotThrowAnyException();
    }
}
