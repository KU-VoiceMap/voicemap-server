package org.ku.voicemap.domain.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.DocumentResult;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.config.AiInstructionProperties.Instructions;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.document.dto.CreateDocumentResponse;
import org.ku.voicemap.domain.document.entity.Document;
import org.ku.voicemap.domain.document.entity.DocumentKeyword;
import org.ku.voicemap.domain.document.repository.DocumentKeywordRepository;
import org.ku.voicemap.domain.document.repository.DocumentRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentKeywordRepository documentKeywordRepository;

    @Mock
    private ScriptRepository scriptRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AiInstructionProperties aiInstructionProperties;

    private DocumentService documentService;

    private static final String MEMBER_NUMBER = "member-123";
    private static final String CHAT_ID = "chat-456";
    private static final String DOCUMENT_INSTRUCTION = "document instruction";

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
            documentRepository, documentKeywordRepository, scriptRepository,
            chatRepository, aiChatClient, aiInstructionProperties
        );
    }

    @Test
    void createDocument_uses_aiChatClient_generateDocument() {
        Chat chat = mock(Chat.class);
        when(chat.getMemberNumber()).thenReturn(MEMBER_NUMBER);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        Script script = mock(Script.class);
        when(script.getQuestion()).thenReturn("질문입니다");
        when(script.isAnswered()).thenReturn(true);
        when(script.getAnswer()).thenReturn("답변입니다");
        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of(script));

        when(documentRepository.findAllByMemberNumber(MEMBER_NUMBER)).thenReturn(List.of());

        Instructions instructions = new Instructions("agent", DOCUMENT_INSTRUCTION, "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        DocumentResult aiResult = new DocumentResult(
            "Generated Title", "Generated Summary", "Generated Content", List.of("keyword1", "keyword2")
        );
        when(aiChatClient.generateDocument(eq(DOCUMENT_INSTRUCTION), anyString(), anyList()))
            .thenReturn(aiResult);

        Document savedDocument = mock(Document.class);
        when(savedDocument.getId()).thenReturn("doc-1");
        when(savedDocument.getTitle()).thenReturn("Generated Title");
        when(savedDocument.getSummary()).thenReturn("Generated Summary");
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        DocumentKeyword savedKeyword1 = mock(DocumentKeyword.class);
        when(savedKeyword1.getName()).thenReturn("keyword1");
        DocumentKeyword savedKeyword2 = mock(DocumentKeyword.class);
        when(savedKeyword2.getName()).thenReturn("keyword2");
        when(documentKeywordRepository.save(any(DocumentKeyword.class)))
            .thenReturn(savedKeyword1, savedKeyword2);

        CreateDocumentResponse response = documentService.createDocument(MEMBER_NUMBER, CHAT_ID, LocalDateTime.now());

        verify(aiChatClient).generateDocument(eq(DOCUMENT_INSTRUCTION), anyString(), anyList());
        assertThat(response.title()).isEqualTo("Generated Title");
        assertThat(response.summary()).isEqualTo("Generated Summary");
    }

    @Test
    void createDocument_builds_prompt_with_conversation_text() {
        Chat chat = mock(Chat.class);
        when(chat.getMemberNumber()).thenReturn(MEMBER_NUMBER);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        Script script = mock(Script.class);
        when(script.getQuestion()).thenReturn("테스트 질문");
        when(script.isAnswered()).thenReturn(true);
        when(script.getAnswer()).thenReturn("테스트 답변");
        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of(script));

        when(documentRepository.findAllByMemberNumber(MEMBER_NUMBER)).thenReturn(List.of());

        Instructions instructions = new Instructions("agent", DOCUMENT_INSTRUCTION, "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        DocumentResult aiResult = new DocumentResult("Title", "Summary", "Content", List.of());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(aiChatClient.generateDocument(anyString(), promptCaptor.capture(), anyList()))
            .thenReturn(aiResult);

        Document savedDocument = mock(Document.class);
        when(savedDocument.getId()).thenReturn("doc-2");
        when(savedDocument.getTitle()).thenReturn("Title");
        when(savedDocument.getSummary()).thenReturn("Summary");
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        documentService.createDocument(MEMBER_NUMBER, CHAT_ID, LocalDateTime.now());

        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("테스트 질문");
        assertThat(capturedPrompt).contains("테스트 답변");
    }

    @Test
    void createDocument_throws_when_no_scripts() {
        Chat chat = mock(Chat.class);
        when(chat.getMemberNumber()).thenReturn(MEMBER_NUMBER);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> documentService.createDocument(MEMBER_NUMBER, CHAT_ID, LocalDateTime.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("대화 내용이 없습니다.");
    }

    @Test
    void createDocument_includes_existing_keywords_in_prompt_when_present() {
        Chat chat = mock(Chat.class);
        when(chat.getMemberNumber()).thenReturn(MEMBER_NUMBER);
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        Script script = mock(Script.class);
        when(script.getQuestion()).thenReturn("질문");
        when(script.isAnswered()).thenReturn(false);
        when(scriptRepository.findAllByChatIdOrderByCreatedAt(CHAT_ID)).thenReturn(List.of(script));

        Document existingDoc = mock(Document.class);
        when(existingDoc.getId()).thenReturn("existing-doc");
        when(documentRepository.findAllByMemberNumber(MEMBER_NUMBER)).thenReturn(List.of(existingDoc));
        when(documentKeywordRepository.findDistinctNamesByDocumentIdIn(List.of("existing-doc")))
            .thenReturn(List.of("기존키워드"));

        Instructions instructions = new Instructions("agent", DOCUMENT_INSTRUCTION, "chat", "ctx");
        when(aiInstructionProperties.instructions()).thenReturn(instructions);

        DocumentResult aiResult = new DocumentResult("Title", "Summary", "Content", List.of());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(aiChatClient.generateDocument(anyString(), promptCaptor.capture(), anyList()))
            .thenReturn(aiResult);

        Document savedDocument = mock(Document.class);
        when(savedDocument.getId()).thenReturn("doc-3");
        when(savedDocument.getTitle()).thenReturn("Title");
        when(savedDocument.getSummary()).thenReturn("Summary");
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        documentService.createDocument(MEMBER_NUMBER, CHAT_ID, LocalDateTime.now());

        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("기존키워드");
    }
}
