package org.ku.voicemap.service.chat;

import org.springframework.stereotype.Component;

// TODO: Config로 관리
@Component
public class EllipsisTitleSummarizer implements ChatTitleSummarizer {

    private static final int MAX_LENGTH = 10;

    @Override
    public String summarize(String question) {
        if (question.length() <= MAX_LENGTH) {
            return question;
        }
        return question.substring(0, MAX_LENGTH) + "...";
    }
}
