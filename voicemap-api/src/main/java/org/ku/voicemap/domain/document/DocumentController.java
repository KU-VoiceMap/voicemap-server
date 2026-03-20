package org.ku.voicemap.domain.document;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.config.AuthenticatedMember;
import org.ku.voicemap.domain.document.dto.CreateDocumentRequest;
import org.ku.voicemap.domain.document.dto.CreateDocumentResponse;
import org.ku.voicemap.domain.document.dto.DocumentDetailResponse;
import org.ku.voicemap.domain.document.dto.DocumentGraphResponse;
import org.ku.voicemap.domain.document.dto.DocumentListResponse;
import org.ku.voicemap.domain.document.dto.KeywordListResponse;
import org.ku.voicemap.domain.document.service.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/documents")
    public CreateDocumentResponse createDocument(
            @AuthenticatedMember String memberNumber,
            @Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(memberNumber, request.chatId(), LocalDateTime.now());
    }

    @GetMapping("/documents")
    public DocumentListResponse getDocuments(@AuthenticatedMember String memberNumber) {
        return documentService.getDocuments(memberNumber);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentDetailResponse getDocument(
            @AuthenticatedMember String memberNumber,
            @PathVariable String documentId) {
        return documentService.getDocument(memberNumber, documentId);
    }

    @GetMapping("/documents/graph")
    public DocumentGraphResponse getDocumentGraph(@AuthenticatedMember String memberNumber) {
        return documentService.getDocumentGraph(memberNumber);
    }

    @GetMapping("/keywords")
    public KeywordListResponse getKeywords(@AuthenticatedMember String memberNumber) {
        return documentService.getKeywords(memberNumber);
    }
}
