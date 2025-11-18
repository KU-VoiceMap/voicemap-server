package org.ku.voicemap.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Transactional(readOnly = true)
    List<Chat> findAllByMemberId(long memberId);
}
