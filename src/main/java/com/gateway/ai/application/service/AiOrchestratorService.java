package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;

public class AiOrchestratorService implements ProcessPromptUseCase {
    private final LlmServicePort llmServicePort;

    public AiOrchestratorService(LlmServicePort llmServicePort) {
        this.llmServicePort = llmServicePort;
    }

    @Override
    public AiAnswer process(PromptQuery promptQuery) {
        String response = llmServicePort.generate(promptQuery.getPrompt());
        return new AiAnswer(response, "NONE");
    }
}
