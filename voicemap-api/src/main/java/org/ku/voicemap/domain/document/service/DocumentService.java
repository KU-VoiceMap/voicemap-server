package org.ku.voicemap.domain.document.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.chat.DocumentResult;
import org.ku.voicemap.config.AiInstructionProperties;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.repository.ChatRepository;
import org.ku.voicemap.domain.document.dto.CreateDocumentResponse;
import org.ku.voicemap.domain.document.dto.DocumentDetailResponse;
import org.ku.voicemap.domain.document.dto.DocumentGraphResponse;
import org.ku.voicemap.domain.document.dto.DocumentGraphResponse.EdgeResponse;
import org.ku.voicemap.domain.document.dto.DocumentGraphResponse.NodeResponse;
import org.ku.voicemap.domain.document.dto.DocumentListResponse;
import org.ku.voicemap.domain.document.dto.DocumentListResponse.DocumentSummary;
import org.ku.voicemap.domain.document.dto.KeywordListResponse;
import org.ku.voicemap.domain.document.dto.KeywordListResponse.KeywordSummary;
import org.ku.voicemap.domain.document.entity.Document;
import org.ku.voicemap.domain.document.entity.DocumentKeyword;
import org.ku.voicemap.domain.document.repository.DocumentKeywordRepository;
import org.ku.voicemap.domain.document.repository.DocumentRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentKeywordRepository documentKeywordRepository;
    private final ScriptRepository scriptRepository;
    private final ChatRepository chatRepository;
    private final AiChatClient aiChatClient;
    private final AiInstructionProperties aiInstructionProperties;

    @Transactional
    public CreateDocumentResponse createDocument(String memberNumber, String chatId, LocalDateTime now) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅이 존재하지 않습니다."));
        if (!chat.getMemberNumber().equals(memberNumber)) {
            throw new IllegalArgumentException("해당 채팅이 존재하지 않습니다.");
        }

        List<Script> scripts = scriptRepository.findAllByChatIdOrderByCreatedAt(chatId);
        if (scripts.isEmpty()) {
            throw new IllegalArgumentException("대화 내용이 없습니다.");
        }

        String conversationText = buildConversationText(scripts);
        List<String> existingKeywords = findMemberKeywords(memberNumber);

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자와 AI의 대화 기록입니다. 이 대화를 바탕으로 문서를 작성하고 키워드를 추출해주세요.\n\n");
        if (!existingKeywords.isEmpty()) {
            prompt.append("[기존 키워드 목록]: ").append(String.join(", ", existingKeywords)).append("\n");
            prompt.append("※ 가능하면 기존 키워드를 재사용하고, 새로운 개념만 신규 키워드로 추출하세요.\n\n");
        }
        prompt.append("[대화 기록]\n").append(conversationText);

        String instruction = aiInstructionProperties.instructions().document();
        DocumentResult aiResult = aiChatClient.generateDocument(instruction, prompt.toString(), existingKeywords);

        Document document = documentRepository.save(
                new Document(chatId, memberNumber, aiResult.title(), aiResult.summary(), aiResult.content(), now)
        );

        List<String> savedKeywordNames = saveKeywords(document.getId(), aiResult.keywords());

        return new CreateDocumentResponse(document.getId(), document.getTitle(), document.getSummary(), savedKeywordNames);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getDocuments(String memberNumber) {
        List<Document> documents = documentRepository.findAllByMemberNumber(memberNumber);
        return new DocumentListResponse(
                documents.stream()
                        .map(doc -> new DocumentSummary(doc.getId(), doc.getTitle(), doc.getSummary(), doc.getChatId(), doc.getCreatedAt()))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(String memberNumber, String documentId) {
        Document document = findDocumentByMember(memberNumber, documentId);

        List<String> keywordNames = documentKeywordRepository.findAllByDocumentId(documentId).stream()
                .map(DocumentKeyword::getName)
                .toList();

        return new DocumentDetailResponse(
                document.getId(),
                document.getChatId(),
                document.getTitle(),
                document.getSummary(),
                document.getContent(),
                keywordNames,
                document.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public DocumentGraphResponse getDocumentGraph(String memberNumber) {
        List<Document> documents = documentRepository.findAllByMemberNumber(memberNumber);

        if (documents.isEmpty()) {
            return new DocumentGraphResponse(List.of(), List.of());
        }

        List<String> documentIds = documents.stream().map(Document::getId).toList();

        List<NodeResponse> documentNodes = documents.stream()
                .map(doc -> new NodeResponse(doc.getId(), "DOCUMENT", doc.getTitle(), null))
                .toList();

        List<DocumentKeyword> allDocumentKeywords = documentKeywordRepository.findAllByDocumentIdIn(documentIds);

        List<String> distinctKeywordNames = allDocumentKeywords.stream()
                .map(DocumentKeyword::getName)
                .distinct()
                .toList();

        List<NodeResponse> keywordNodes = distinctKeywordNames.stream()
                .map(name -> new NodeResponse(name, "KEYWORD", null, name))
                .toList();

        List<EdgeResponse> edges = allDocumentKeywords.stream()
                .map(dk -> new EdgeResponse(dk.getDocumentId(), dk.getName()))
                .toList();

        List<NodeResponse> allNodes = new java.util.ArrayList<>(documentNodes);
        allNodes.addAll(keywordNodes);

        return new DocumentGraphResponse(allNodes, edges);
    }

    @Transactional(readOnly = true)
    public KeywordListResponse getKeywords(String memberNumber) {
        List<Document> documents = documentRepository.findAllByMemberNumber(memberNumber);
        List<String> documentIds = documents.stream().map(Document::getId).toList();

        if (documentIds.isEmpty()) {
            return new KeywordListResponse(List.of());
        }

        List<DocumentKeyword> allDocumentKeywords = documentKeywordRepository.findAllByDocumentIdIn(documentIds);

        List<String> distinctKeywordNames = allDocumentKeywords.stream()
                .map(DocumentKeyword::getName)
                .distinct()
                .toList();

        return new KeywordListResponse(
                distinctKeywordNames.stream()
                        .map(name -> {
                            long documentCount = allDocumentKeywords.stream()
                                    .filter(dk -> dk.getName().equals(name))
                                    .map(DocumentKeyword::getDocumentId)
                                    .distinct()
                                    .count();
                            return new KeywordSummary(name, documentCount);
                        })
                        .toList()
        );
    }

    private String buildConversationText(List<Script> scripts) {
        StringBuilder sb = new StringBuilder();
        for (Script script : scripts) {
            sb.append("Q: ").append(script.getQuestion()).append("\n");
            if (script.isAnswered()) {
                sb.append("A: ").append(script.getAnswer()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<String> findMemberKeywords(String memberNumber) {
        List<Document> documents = documentRepository.findAllByMemberNumber(memberNumber);
        List<String> documentIds = documents.stream().map(Document::getId).toList();

        if (documentIds.isEmpty()) {
            return List.of();
        }

        return documentKeywordRepository.findDistinctNamesByDocumentIdIn(documentIds);
    }

    private List<String> saveKeywords(String documentId, List<String> keywordNames) {
        return keywordNames.stream()
                .map(name -> {
                    DocumentKeyword documentKeyword = documentKeywordRepository.save(
                            new DocumentKeyword(documentId, name)
                    );
                    return documentKeyword.getName();
                })
                .toList();
    }

    private Document findDocumentByMember(String memberNumber, String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다."));
        if (!document.getMemberNumber().equals(memberNumber)) {
            throw new IllegalArgumentException("해당 문서가 존재하지 않습니다.");
        }
        return document;
    }
}
