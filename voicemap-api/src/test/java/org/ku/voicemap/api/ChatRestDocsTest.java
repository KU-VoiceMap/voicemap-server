package org.ku.voicemap.api;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.Schema;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.domain.chat.dto.ChatDetailsResponse;
import org.ku.voicemap.domain.chat.dto.ChatDetailsResponse.ChatDetailResponse;
import org.ku.voicemap.domain.chat.dto.CreateChatRequest;
import org.ku.voicemap.domain.chat.dto.CreateChatResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse;
import org.ku.voicemap.domain.chat.dto.MemberChatsResponse.MemberChatResponse;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;

class ChatRestDocsTest extends RestDocsTest {

    private static final String JWT_SECRET = "secretsecretsecretsecretsecretsecretsecretsecret";
    private static final String MEMBER_NUMBER = "2501010001";
    private final ResourceSnippetParametersBuilder snippetBuilder = new ResourceSnippetParametersBuilder().tag("채팅 API");

    @Test
    void 채팅을_생성한다() {
        String request = """
            {
              "question": "강남에서 맛집 추천해줘"
            }
            """;

        given(chatService.createChat(eq(MEMBER_NUMBER), any()))
            .willReturn(new CreateChatResponse("chat-1", "강남 맛집 추천"));

        HeaderDescriptor[] requestHeaderDescriptors = {
            headerWithName("Authorization").description("Bearer 액세스 토큰")
        };
        FieldDescriptor[] requestFieldDescriptors = {
            fieldWithPath("question").description("첫 질문")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("chatId").description("채팅 ID"),
            fieldWithPath("title").description("채팅 제목")
        };

        RestDocumentationResultHandler handler = document(
            "chat-create",
            snippetBuilder.description("채팅 생성 API")
                .requestSchema(Schema.schema(CreateChatRequest.class.getSimpleName()))
                .responseSchema(Schema.schema(CreateChatResponse.class.getSimpleName())),
            requestHeaders(requestHeaderDescriptors),
            requestFields(requestFieldDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .header("Authorization", "Bearer " + createAccessToken(MEMBER_NUMBER))
            .body(request)
            .when().post("/chats")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    @Test
    void 회원의_채팅_목록을_조회한다() {
        given(chatService.getMemberChats(MEMBER_NUMBER))
            .willReturn(new MemberChatsResponse(
                List.of(
                    new MemberChatResponse("chat-1", "강남 맛집 추천"),
                    new MemberChatResponse("chat-2", "제주 여행 계획")
                )
            ));

        HeaderDescriptor[] requestHeaderDescriptors = {
            headerWithName("Authorization").description("Bearer 액세스 토큰")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("chats[]").description("채팅 목록"),
            fieldWithPath("chats[].chatId").description("채팅 ID"),
            fieldWithPath("chats[].title").description("채팅 제목")
        };

        RestDocumentationResultHandler handler = document(
            "chat-member-list",
            snippetBuilder.description("회원 채팅 목록 조회 API")
                .responseSchema(Schema.schema(MemberChatsResponse.class.getSimpleName())),
            requestHeaders(requestHeaderDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .header("Authorization", "Bearer " + createAccessToken(MEMBER_NUMBER))
            .when().get("/chats")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    @Test
    void 채팅_상세를_조회한다() {
        given(chatService.getChatDetail(MEMBER_NUMBER, "chat-1"))
            .willReturn(new ChatDetailsResponse(
                List.of(
                    new ChatDetailResponse(
                        "강남 맛집 추천해줘",
                        "첫 번째로 압구정과 삼성역 주변을 추천해요.",
                        LocalDateTime.of(2026, 2, 17, 12, 34, 56)
                    )
                )
            ));

        HeaderDescriptor[] requestHeaderDescriptors = {
            headerWithName("Authorization").description("Bearer 액세스 토큰")
        };
        ParameterDescriptor[] pathParameterDescriptors = {
            parameterWithName("chatId").description("채팅 ID")
        };
        FieldDescriptor[] responseFieldDescriptors = {
            fieldWithPath("scripts[]").description("스크립트 목록"),
            fieldWithPath("scripts[].question").description("사용자 질문"),
            fieldWithPath("scripts[].answer").description("에이전트 응답").optional(),
            fieldWithPath("scripts[].createdAt").description("질문 생성 시각")
        };

        RestDocumentationResultHandler handler = document(
            "chat-detail",
            snippetBuilder.description("채팅 상세 조회 API")
                .responseSchema(Schema.schema(ChatDetailsResponse.class.getSimpleName())),
            requestHeaders(requestHeaderDescriptors),
            pathParameters(pathParameterDescriptors),
            responseFields(responseFieldDescriptors)
        );

        givenWithSpec().log().all()
            .header("Authorization", "Bearer " + createAccessToken(MEMBER_NUMBER))
            .pathParam("chatId", "chat-1")
            .when().get("/chats/{chatId}")
            .then().log().all()
            .apply(handler)
            .statusCode(200);
    }

    private String createAccessToken(String memberNumber) {
        Instant expireAt = Instant.now().plusSeconds(60 * 60);
        return JWT.create()
            .withClaim("type", "ACCESS")
            .withClaim("memberNumber", memberNumber)
            .withExpiresAt(expireAt)
            .sign(Algorithm.HMAC256(JWT_SECRET));
    }
}
