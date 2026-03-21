package org.ku.voicemap.domain.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "document_keyword",
    indexes = {
        @Index(name = "idx_dk_document_id", columnList = "document_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_dk_document_name", columnNames = {"document_id", "name"})
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

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "embedding", nullable = true, length = 4000)
    private String embedding;

    public DocumentKeyword(String documentId, String name) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("문서 ID는 필수 입력 사항입니다.");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("키워드는 필수 입력 사항입니다.");
        }
        this.documentId = documentId;
        this.name = name.trim().toLowerCase();
    }
}
