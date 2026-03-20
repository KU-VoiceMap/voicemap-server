package org.ku.voicemap.domain.document.repository;

import java.util.List;
import org.ku.voicemap.domain.document.entity.DocumentKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentKeywordRepository extends JpaRepository<DocumentKeyword, Long> {

    @Transactional(readOnly = true)
    List<DocumentKeyword> findAllByDocumentId(String documentId);
}
