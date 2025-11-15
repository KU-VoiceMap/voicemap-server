package org.ku.voicemap.domain.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final Random random = new Random();
    private final MemberRepository memberRepository;

    public String generate(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        String memberNumber;
        do {
            memberNumber = date.format(FORMATTER) + generateRandomSuffix(4);
        } while (memberRepository.findByMemberNumber(memberNumber).isPresent());
        return memberNumber;
    }

    private String generateRandomSuffix(int length) {
        return random.ints(length, 0, CHARS.length())
            .mapToObj(CHARS::charAt)
            .map(String::valueOf)
            .collect(Collectors.joining());
    }
}
