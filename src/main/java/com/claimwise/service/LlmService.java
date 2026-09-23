package com.claimwise.service;

public interface LlmService {

    String generateAnswer(String systemPrompt, String userPrompt);
}
