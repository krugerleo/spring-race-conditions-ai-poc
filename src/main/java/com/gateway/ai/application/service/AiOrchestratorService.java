package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.SemanticCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.CacheType;
import com.gateway.ai.domain.model.PromptQuery;
import com.gateway.ai.infrastructure.adapters.out.lock.DistributedLock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

public class AiOrchestratorService implements ProcessPromptUseCase {

    private static final String CACHE_NAME = "exact-cache";

    private final LlmServicePort llmServicePort;
    private final SemanticCachePort semanticCachePort;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public AiOrchestratorService(LlmServicePort llmServicePort,
                                 SemanticCachePort semanticCachePort,
                                 CacheManager cacheManager,
                                 MeterRegistry meterRegistry) {
        this.llmServicePort = llmServicePort;
        this.semanticCachePort = semanticCachePort;
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @DistributedLock(key = "#query.hash", waitTime = 5, leaseTime = 10)
    public AiAnswer process(PromptQuery query) {
        Cache exactCache = cacheManager.getCache(CACHE_NAME);

        // 1. Cache exato (double-check: quem entra no lock depois já encontra o valor).
        String exact = exactCache != null ? exactCache.get(query.getHash(), String.class) : null;
        if (exact != null) {
            meterRegistry.counter("ai.cache.exact.hits").increment();
            return new AiAnswer(exact, CacheType.EXACT_HIT);
        }

        // 2. Cache semântico (prompts diferentes com o mesmo significado).
        Optional<String> semantic = semanticCachePort.findSimilar(query.getPrompt());
        if (semantic.isPresent()) {
            meterRegistry.counter("ai.cache.semantic.hits").increment();
            return new AiAnswer(semantic.get(), CacheType.SEMANTIC_HIT);
        }

        // 3. Cache miss: chama a LLM e atualiza os dois caches.
        meterRegistry.counter("ai.calls.total").increment();
        String response = llmServicePort.generate(query.getPrompt());

        if (exactCache != null) {
            exactCache.put(query.getHash(), response);
        }
        semanticCachePort.store(query.getPrompt(), response);

        return new AiAnswer(response, CacheType.MISS);
    }
}
