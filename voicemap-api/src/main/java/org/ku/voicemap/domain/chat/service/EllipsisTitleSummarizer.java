package org.ku.voicemap.domain.chat.service;

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
