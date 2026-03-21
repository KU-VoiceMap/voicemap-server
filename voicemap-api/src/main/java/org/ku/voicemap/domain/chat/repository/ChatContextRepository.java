package org.ku.voicemap.domain.chat.repository;

import java.util.Optional;
import org.ku.voicemap.domain.chat.entity.ChatContext;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatContextRepository extends JpaRepository<ChatContext, Long> {

    Optional<ChatContext> findFirstByChatIdOrderByCreatedAtDesc(String chatId);
}
