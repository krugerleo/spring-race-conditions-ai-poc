package com.gateway.ai.domain.model;

public class PromptQuery {
    private String prompt;
    private String hash;

    public PromptQuery(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt cannot be null");
        }
        this.prompt = prompt;
        this.hash = Integer.toHexString(this.prompt.hashCode());
    }
    public String getPrompt() {
        return prompt;
    }
    public String getHash() {
        return hash;
    }
}
