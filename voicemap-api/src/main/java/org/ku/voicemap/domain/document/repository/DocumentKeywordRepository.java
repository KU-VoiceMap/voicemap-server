package org.ku.voicemap.domain.document.repository;

import java.util.List;
import org.ku.voicemap.domain.document.entity.DocumentKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentKeywordRepository extends JpaRepository<DocumentKeyword, Long> {

    @Transactional(readOnly = true)
    List<DocumentKeyword> findAllByDocumentId(String documentId);

    @Transactional(readOnly = true)
    List<DocumentKeyword> findAllByDocumentIdIn(List<String> documentIds);

    @Transactional(readOnly = true)
    @Query("SELECT DISTINCT dk.name FROM DocumentKeyword dk WHERE dk.documentId IN :documentIds")
    List<String> findDistinctNamesByDocumentIdIn(List<String> documentIds);
}
