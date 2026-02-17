package org.ku.voicemap.api;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.member.dto.MemberRegisterRequest;
import org.ku.voicemap.domain.member.dto.MemberRegisterResponse;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;

class MemberRegistrationRestDocsTest extends RestDocsTest {

    private final ResourceSnippetParametersBuilder snippetBuilder = new ResourceSnippetParametersBuilder().tag("회원 API");

    @Test
    void 회원을_등록한다() {
        String request = """
            {
              "provider": "GOOGLE",
              "providerToken": "google_provider_token",
              "email": "test@voicemap.org"
            }
            """;

        given(externalMemberInfoResolver.resolve(AuthProvider.GOOGLE, "google_provider_token"))
            .willReturn(new ExternalMember(AuthProvider.GOOGLE, "principal", "test@voicemap.org"));
        given(memberRegistrationService.register(any(ExternalMember.class), any(String.class), any()))
            .willReturn(new MemberRegisterResponse("2501010001", "access-token", "refresh-token"));

        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("provider").description("인증 제공자"),
            fieldWithPath("providerToken").description("제공자 토큰"),
            fieldWithPath("email").description("회원 이메일")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("memberNumber").description("회원 번호"),
            fieldWithPath("accessToken").description("액세스 토큰"),
            fieldWithPath("refreshToken").description("리프레시 토큰")
        };

        RestDocumentationResultHandler handler = document(
            "member-register",
            snippetBuilder.description("회원 등록 API")
                .requestSchema(Schema.schema(MemberRegisterRequest.class.getSimpleName()))
                .responseSchema(Schema.schema(MemberRegisterResponse.class.getSimpleName())),
            requestFields(requestFieldDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .body(request)
            .when().post("/v1/register")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }
}
