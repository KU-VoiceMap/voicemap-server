package org.ku.voicemap.domain.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.service.InvalidTokenException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

class AuthenticatedMemberHandlerMethodArgumentResolverTest {

    private final AuthenticatedMemberHandlerMethodArgumentResolver resolver =
        new AuthenticatedMemberHandlerMethodArgumentResolver("secretKeysecretKeysecretKeysecretKey");

    @SuppressWarnings("unused") // Reflection으로 사용
    private void test(@AuthenticatedMember String memberNumber, String foo) { /* no-op for test */ }

    @Test
    void resolve_타입을_검사한다() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("test", String.class, String.class);
        MethodParameter targetParameter = new MethodParameter(method, 0);
        MethodParameter nonTargetParameter = new MethodParameter(method, 1);

        assertThat(resolver.supportsParameter(targetParameter)).isTrue();
        assertThat(resolver.supportsParameter(nonTargetParameter)).isFalse();
    }

    @Test
    void 토큰이_존재하지_않는_경우_예외를_발생한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        NativeWebRequest webRequest = new ServletWebRequest(request, response);
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void access_타입이_아닌_경우_예외를_발생한다() {
        // { "type": "REFRESH", "memberNumber": "123456" }
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXBlIjoiUkVGUkVTSCIsIm1lbWJlck51bWJlciI6IjEyMzQ1NiJ9.GF8LBCa3JtC47Lu4JuJ0RoA2HTjFKS-8uS8jhRDL4h8";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        NativeWebRequest webRequest = new ServletWebRequest(request, response);
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 기한이_지난_경우_예외를_발생한다() {
        // { "type": "ACCESS", "memberNumber": "123456", "exp": 0 }
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXBlIjoiQUNDRVNTIiwibWVtYmVyTnVtYmVyIjoiMTIzNDU2IiwiZXhwIjowfQ.DqI35B9Wk7S4qqaXSXpspsYiYZbsOT0ETZJpvJuxaS0";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        NativeWebRequest webRequest = new ServletWebRequest(request, response);
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 토큰_값을_변환한다() {
        // { "type: "ACCESS", "memberNumber": "123456", "exp": 32531258622 }
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXBlIjoiQUNDRVNTIiwibWVtYmVyTnVtYmVyIjoiMTIzNDU2IiwiZXhwIjozMjUzMTI1ODYyMn0.hJzZlQn36hsp8E9l_ACUd89WRo8XN30tTG3lqOOijoQ";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        NativeWebRequest webRequest = new ServletWebRequest(request, response);
        assertThat(resolver.resolveArgument(null, null, webRequest, null))
            .isEqualTo("123456");
    }
}
