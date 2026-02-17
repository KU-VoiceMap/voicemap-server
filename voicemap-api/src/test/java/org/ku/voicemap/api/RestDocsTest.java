package org.ku.voicemap.api;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ku.voicemap.domain.auth.AuthController;
import org.ku.voicemap.domain.auth.service.AuthService;
import org.ku.voicemap.domain.auth.service.ExternalMemberInfoResolver;
import org.ku.voicemap.domain.chat.ChatController;
import org.ku.voicemap.domain.chat.service.ChatService;
import org.ku.voicemap.domain.member.MemberRegistrationController;
import org.ku.voicemap.domain.member.service.MemberRegistrationService;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcOperationPreprocessorsConfigurer;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest({
    AuthController.class,
    MemberRegistrationController.class,
    ChatController.class
})
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsTest {

    private MockMvcRequestSpecification spec;

    @MockitoBean
    protected ExternalMemberInfoResolver externalMemberInfoResolver;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected MemberRegistrationService memberRegistrationService;

    @MockitoBean
    protected ChatService chatService;

    @BeforeEach
    void setUpRestDocs(WebApplicationContext context, RestDocumentationContextProvider provider) {
        UriModifyingOperationPreprocessor uriModifier = modifyUris()
            .scheme("https")
            .host("api.voicemap.local")
            .removePort();
        HeadersModifyingOperationPreprocessor requestHeaderModifier = modifyHeaders()
            .set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .remove(HttpHeaders.CONTENT_LENGTH);
        HeadersModifyingOperationPreprocessor responseHeaderModifier = modifyHeaders()
            .remove(HttpHeaders.CONTENT_LENGTH)
            .remove(HttpHeaders.CONNECTION)
            .remove(HttpHeaders.TRANSFER_ENCODING)
            .remove(HttpHeaders.VARY)
            .remove("Keep-Alive");

        MockMvcOperationPreprocessorsConfigurer configurer = documentationConfiguration(provider)
            .operationPreprocessors()
            .withRequestDefaults(prettyPrint(), uriModifier, requestHeaderModifier)
            .withResponseDefaults(prettyPrint(), responseHeaderModifier);

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(configurer)
            .build();

        spec = RestAssuredMockMvc.given()
            .mockMvc(mockMvc);
    }

    protected MockMvcRequestSpecification givenWithSpec() {
        return spec.contentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
