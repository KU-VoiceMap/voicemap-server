package org.ku.voicemap.domain.document.repository;

import java.util.List;
import org.ku.voicemap.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    @Transactional(readOnly = true)
    List<Document> findAllByMemberNumber(String memberNumber);

    @Transactional(readOnly = true)
    List<Document> findAllByChatId(String chatId);
}
