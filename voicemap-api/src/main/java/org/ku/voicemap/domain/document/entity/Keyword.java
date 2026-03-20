package org.ku.voicemap.domain.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Table(
    catalog = "voicemap",
    name = "keyword"
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "embedding", nullable = true, length = 4000)
    private String embedding;

    public Keyword(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("키워드는 필수 입력 사항입니다.");
        }
        this.name = name.trim().toLowerCase();
    }
}
