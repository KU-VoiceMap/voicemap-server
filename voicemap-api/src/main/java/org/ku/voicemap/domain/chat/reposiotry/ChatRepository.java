package org.ku.voicemap.domain.chat.reposiotry;

import java.util.List;
import org.ku.voicemap.domain.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChatRepository extends JpaRepository<Chat, String> {

    @Transactional(readOnly = true)
    List<Chat> findAllByMemberNumberOrderByLastInteractedAtDesc(String memberNumber);
}
