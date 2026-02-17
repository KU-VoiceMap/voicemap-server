package org.ku.voicemap.api;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.dto.LogoutRequest;
import org.ku.voicemap.domain.auth.dto.TokenRequest;
import org.ku.voicemap.domain.auth.dto.TokenRotateRequest;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.service.AuthClientNotConnectedException;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;

class AuthRestDocsTest extends RestDocsTest {

    private final ResourceSnippetParametersBuilder snippetBuilder = new ResourceSnippetParametersBuilder().tag("인증 API");

    @Test
    void 로그인한다() {
        String request = """
            {
              "provider": "GOOGLE",
              "providerToken": "google_provider_token"
            }
            """;

        given(externalMemberInfoResolver.resolve(AuthProvider.GOOGLE, "google_provider_token"))
            .willReturn(new ExternalMember(AuthProvider.GOOGLE, "principal", "test@voicemap.org"));
        given(authService.login(any(ExternalMember.class), any()))
            .willReturn(new TokenResponse("access-token", "refresh-token"));

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("provider").description("인증 제공자"),
            fieldWithPath("providerToken").description("제공자 토큰")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("accessToken").description("액세스 토큰"),
            fieldWithPath("refreshToken").description("리프레시 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "auth-login",
            snippetBuilder.description("SNS 로그인 API")
                .requestSchema(Schema.schema(TokenRequest.class.getSimpleName()))
                .responseSchema(Schema.schema(TokenResponse.class.getSimpleName())),
            requestFields(requestFieldDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/auth/login")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    @Test
    void 연결된_회원이_없으면_로그인_요청에_실패한다() {
        String request = """
            {
              "provider": "GOOGLE",
              "providerToken": "google_provider_token"
            }
            """;

        given(externalMemberInfoResolver.resolve(AuthProvider.GOOGLE, "google_provider_token"))
            .willReturn(new ExternalMember(AuthProvider.GOOGLE, "principal", "test@voicemap.org"));
        given(authService.login(any(ExternalMember.class), any()))
            .willThrow(new AuthClientNotConnectedException());

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("provider").description("인증 제공자"),
            fieldWithPath("providerToken").description("제공자 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "auth-login-not-found-member",
            snippetBuilder.description("SNS 로그인 API")
                .requestSchema(Schema.schema(TokenRequest.class.getSimpleName())),
            requestFields(requestFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/auth/login")
            .then().log().all()
            .apply(handler)
            .statusCode(404);
    }

    @Test
    void 로그아웃한다() {
        String request = """
            {
              "refreshToken": "refresh-token"
            }
            """;

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("refreshToken").description("리프레시 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "auth-logout",
            snippetBuilder.description("로그아웃 API")
                .requestSchema(Schema.schema(LogoutRequest.class.getSimpleName())),
            requestFields(requestFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/auth/logout")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    @Test
    void 액세스_토큰을_재발급한다() {
        String request = """
            {
              "refreshToken": "refresh-token"
            }
            """;

        given(authService.rotateAccessToken(eq("refresh-token"), any()))
            .willReturn(new TokenResponse("new-access-token", "refresh-token"));

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("refreshToken").description("리프레시 토큰")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("accessToken").description("새 액세스 토큰"),
            fieldWithPath("refreshToken").description("기존 리프레시 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "auth-access-rotate",
            snippetBuilder.description("액세스 토큰 재발급 API")
                .requestSchema(Schema.schema(TokenRotateRequest.class.getSimpleName()))
                .responseSchema(Schema.schema(TokenResponse.class.getSimpleName())),
            requestFields(requestFieldDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/auth/access")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    @Test
    void 리프레시_토큰을_재발급한다() {
        String request = """
            {
              "refreshToken": "refresh-token"
            }
            """;

        given(authService.rotateRefreshToken(eq("refresh-token"), any()))
            .willReturn(new TokenResponse("new-access-token", "new-refresh-token"));

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("refreshToken").description("리프레시 토큰")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("accessToken").description("새 액세스 토큰"),
            fieldWithPath("refreshToken").description("새 리프레시 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "auth-refresh-rotate",
            snippetBuilder.description("리프레시 토큰 재발급 API")
                .requestSchema(Schema.schema(TokenRotateRequest.class.getSimpleName()))
                .responseSchema(Schema.schema(TokenResponse.class.getSimpleName())),
            requestFields(requestFieldDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/auth/refresh")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }
}
