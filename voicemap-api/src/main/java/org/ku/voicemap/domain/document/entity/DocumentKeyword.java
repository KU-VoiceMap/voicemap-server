package org.ku.voicemap.domain.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
    catalog = "voicemap",
    name = "document_keyword",
    indexes = {
        @Index(name = "idx_dk_document_id", columnList = "document_id"),
        @Index(name = "idx_dk_keyword_id", columnList = "keyword_id")
    }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    public DocumentKeyword(String documentId, Long keywordId) {
        this.documentId = documentId;
        this.keywordId = keywordId;
    }
}
