package com.gateway.ai.application.ports.out;

public interface LlmServicePort {
    String generate(String prompt);
}
