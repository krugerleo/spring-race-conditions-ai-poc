package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.PromptCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;
import com.gateway.ai.infrastructure.adapters.out.lock.DistributedLock;

public class AiGenerationService {

    private final LlmServicePort llmServicePort;
    private final PromptCachePort promptCachePort;

    public AiGenerationService(LlmServicePort llmServicePort, PromptCachePort promptCachePort) {
        this.llmServicePort = llmServicePort;
        this.promptCachePort = promptCachePort;
    }

    @DistributedLock(key = "#query.hash", waitTime = 5, leaseTime = 10)
    public AiAnswer generate(PromptQuery query) {
        // Double-check: outra thread pode ter gerado e gravado enquanto esperávamos o lock.
        return promptCachePort.find(query.getHash())
                .map(cached -> new AiAnswer(cached.answer(), AiAnswer.CACHE_TYPE_EXACT))
                .orElseGet(() -> generateAndCache(query));
    }

    private AiAnswer generateAndCache(PromptQuery query) {
        String response = llmServicePort.generate(query.getPrompt());
        AiAnswer answer = new AiAnswer(response, AiAnswer.CACHE_TYPE_NONE);
        promptCachePort.put(query.getHash(), answer);
        return answer;
    }
}
