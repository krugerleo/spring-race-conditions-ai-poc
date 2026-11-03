package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.PromptCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;

public class AiOrchestratorService implements ProcessPromptUseCase {

    private final AiGenerationService aiGenerationService;
    private final PromptCachePort promptCachePort;

    public AiOrchestratorService(AiGenerationService aiGenerationService, PromptCachePort promptCachePort) {
        this.aiGenerationService = aiGenerationService;
        this.promptCachePort = promptCachePort;
    }

    @Override
    public AiAnswer process(PromptQuery promptQuery) {
        // Cache hit não passa pelo lock nem pela LLM.
        return promptCachePort.find(promptQuery.getHash())
                .map(cached -> new AiAnswer(cached.answer(), AiAnswer.CACHE_TYPE_EXACT))
                .orElseGet(() -> aiGenerationService.generate(promptQuery));
    }
}
