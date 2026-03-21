package org.ku.voicemap.domain.chat.service;

public interface IdeaContextSummarizer {

    String summarize(String previousContext, String conversation);
}
