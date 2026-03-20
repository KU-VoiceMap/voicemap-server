package org.ku.voicemap.domain.document.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.ku.voicemap.domain.chat.reposiotry.ChatRepository;
import org.ku.voicemap.domain.document.ai.DocumentAiClient;
import org.ku.voicemap.domain.document.ai.DocumentAiResult;
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
import org.ku.voicemap.domain.document.entity.Keyword;
import org.ku.voicemap.domain.document.repository.DocumentKeywordRepository;
import org.ku.voicemap.domain.document.repository.DocumentRepository;
import org.ku.voicemap.domain.document.repository.KeywordRepository;
import org.ku.voicemap.domain.script.Script;
import org.ku.voicemap.domain.script.ScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KeywordRepository keywordRepository;
    private final DocumentKeywordRepository documentKeywordRepository;
    private final ScriptRepository scriptRepository;
    private final ChatRepository chatRepository;
    private final DocumentAiClient documentAiClient;

    @Transactional
    public CreateDocumentResponse createDocument(String memberNumber, String chatId, LocalDateTime now) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅이 존재하지 않습니다."));
        if (!chat.getMemberNumber().equals(memberNumber)) {
            throw new IllegalArgumentException("해당 채팅이 존재하지 않습니다.");
        }

        List<Script> scripts = scriptRepository.findAllByChatId(chatId);
        if (scripts.isEmpty()) {
            throw new IllegalArgumentException("대화 내용이 없습니다.");
        }

        String conversationText = buildConversationText(scripts);
        List<String> existingKeywords = findMemberKeywords(memberNumber);

        DocumentAiResult aiResult = documentAiClient.generate(conversationText, existingKeywords);

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

        List<DocumentKeyword> documentKeywords = documentKeywordRepository.findAllByDocumentId(documentId);
        List<Long> keywordIds = documentKeywords.stream().map(DocumentKeyword::getKeywordId).toList();
        List<String> keywordNames = keywordRepository.findAllById(keywordIds).stream()
                .map(Keyword::getName)
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

        List<NodeResponse> documentNodes = documents.stream()
                .map(doc -> new NodeResponse(doc.getId(), "DOCUMENT", doc.getTitle(), null))
                .toList();

        List<String> documentIds = documents.stream().map(Document::getId).toList();

        List<DocumentKeyword> allDocumentKeywords = documentIds.stream()
                .flatMap(docId -> documentKeywordRepository.findAllByDocumentId(docId).stream())
                .toList();

        List<Long> keywordIds = allDocumentKeywords.stream()
                .map(DocumentKeyword::getKeywordId)
                .distinct()
                .toList();

        List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

        List<NodeResponse> keywordNodes = keywords.stream()
                .map(kw -> new NodeResponse(String.valueOf(kw.getId()), "KEYWORD", null, kw.getName()))
                .toList();

        List<EdgeResponse> edges = allDocumentKeywords.stream()
                .map(dk -> new EdgeResponse(dk.getDocumentId(), dk.getKeywordId()))
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

        List<DocumentKeyword> allDocumentKeywords = documentIds.stream()
                .flatMap(docId -> documentKeywordRepository.findAllByDocumentId(docId).stream())
                .toList();

        List<Long> keywordIds = allDocumentKeywords.stream()
                .map(DocumentKeyword::getKeywordId)
                .distinct()
                .toList();

        List<Keyword> keywords = keywordRepository.findAllById(keywordIds);

        return new KeywordListResponse(
                keywords.stream()
                        .map(keyword -> {
                            long documentCount = allDocumentKeywords.stream()
                                    .filter(dk -> dk.getKeywordId().equals(keyword.getId()))
                                    .map(DocumentKeyword::getDocumentId)
                                    .distinct()
                                    .count();
                            return new KeywordSummary(keyword.getId(), keyword.getName(), documentCount);
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

        return documentIds.stream()
                .flatMap(docId -> documentKeywordRepository.findAllByDocumentId(docId).stream())
                .map(DocumentKeyword::getKeywordId)
                .distinct()
                .map(keywordId -> keywordRepository.findById(keywordId).orElse(null))
                .filter(Objects::nonNull)
                .map(Keyword::getName)
                .toList();
    }

    private List<String> saveKeywords(String documentId, List<String> keywordNames) {
        return keywordNames.stream()
                .map(name -> {
                    String normalized = name.trim().toLowerCase();
                    Keyword keyword = keywordRepository.findByName(normalized)
                            .orElseGet(() -> keywordRepository.save(new Keyword(normalized)));
                    documentKeywordRepository.save(new DocumentKeyword(documentId, keyword.getId()));
                    return keyword.getName();
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
