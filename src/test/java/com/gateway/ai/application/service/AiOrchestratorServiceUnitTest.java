package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.SemanticCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.CacheType;
import com.gateway.ai.domain.model.PromptQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

class AiOrchestratorServiceUnitTest {

    @Test
    void shouldCallLlmOnlyOnceAndServeSecondCallFromExactCache() {
        LlmServicePort llmMock = Mockito.mock(LlmServicePort.class);
        Mockito.when(llmMock.generate(anyString())).thenReturn("Resposta mockada com sucesso!");

        SemanticCachePort semanticCache = Mockito.mock(SemanticCachePort.class);
        Mockito.when(semanticCache.findSimilar(anyString())).thenReturn(Optional.empty());

        CacheManager cacheManager = new ConcurrentMapCacheManager("exact-cache");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiOrchestratorService service =
                new AiOrchestratorService(llmMock, semanticCache, cacheManager, meterRegistry);
        PromptQuery query = new PromptQuery("Teste de prompt");

        AiAnswer first = service.process(query);
        AiAnswer second = service.process(query);

        assertEquals("Resposta mockada com sucesso!", first.answer());
        assertEquals(CacheType.MISS, first.cacheType());
        assertEquals("Resposta mockada com sucesso!", second.answer());
        assertEquals(CacheType.EXACT_HIT, second.cacheType());

        Mockito.verify(llmMock, Mockito.times(1)).generate(anyString());
        assertEquals(1.0, meterRegistry.counter("ai.calls.total").count());
        assertEquals(1.0, meterRegistry.counter("ai.cache.exact.hits").count());
    }

    @Test
    void shouldServeSimilarPromptFromSemanticCache() {
        LlmServicePort llmMock = Mockito.mock(LlmServicePort.class);
        SemanticCachePort semanticCache = Mockito.mock(SemanticCachePort.class);
        Mockito.when(semanticCache.findSimilar(anyString())).thenReturn(Optional.of("Resposta semântica"));

        CacheManager cacheManager = new ConcurrentMapCacheManager("exact-cache");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiOrchestratorService service =
                new AiOrchestratorService(llmMock, semanticCache, cacheManager, meterRegistry);

        AiAnswer answer = service.process(new PromptQuery("Prompt parecido"));

        assertEquals("Resposta semântica", answer.answer());
        assertEquals(CacheType.SEMANTIC_HIT, answer.cacheType());
        Mockito.verifyNoInteractions(llmMock);
        assertEquals(1.0, meterRegistry.counter("ai.cache.semantic.hits").count());
    }
}
