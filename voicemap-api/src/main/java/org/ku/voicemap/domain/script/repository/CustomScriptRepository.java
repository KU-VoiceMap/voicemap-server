package org.ku.voicemap.domain.script.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.script.entity.Script;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class CustomScriptRepository implements ScriptRepositoryCustomInterface {

    private final EntityManager em;

    @Override
    public List<Script> findScriptsPagination(UUID chatId, Long lastId, int size) {

        String jpql;

        if (lastId == null) {
            jpql = "SELECT s FROM Script s WHERE s.chatId = :chatId  ORDER BY s.id DESC";
        } else {
            jpql= "SELECT s FROM Script s WHERE s.chatId = :chatId AND s.id < :lastId ORDER BY s.id DESC";
        }

        TypedQuery<Script> query = em.createQuery(jpql, Script.class)
            .setParameter("chatId", chatId)
            .setMaxResults(size);

        if (lastId != null) {
            query.setParameter("lastId", lastId);
        }

        return query.getResultList();
    }

}
