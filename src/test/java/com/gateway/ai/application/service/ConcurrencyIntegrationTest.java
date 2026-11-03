package com.gateway.ai.application.service;

import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.PromptCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

class AiOrchestratorServiceUnitTest {

    @Test
    void shouldProcessPromptUsingMockedLlm() {
        LlmServicePort llmMock = Mockito.mock(LlmServicePort.class);
        Mockito.when(llmMock.generate(anyString())).thenReturn("Resposta mockada com sucesso!");

        PromptCachePort cache = new InMemoryPromptCache();
        AiGenerationService generationService = new AiGenerationService(llmMock, cache);
        AiOrchestratorService service = new AiOrchestratorService(generationService, cache);
        PromptQuery query = new PromptQuery("Teste de prompt");

        AiAnswer first = service.process(query);
        assertEquals("Resposta mockada com sucesso!", first.answer());
        assertEquals(AiAnswer.CACHE_TYPE_NONE, first.cacheType());

        AiAnswer second = service.process(query);
        assertEquals("Resposta mockada com sucesso!", second.answer());
        assertEquals(AiAnswer.CACHE_TYPE_EXACT, second.cacheType());

        Mockito.verify(llmMock, Mockito.times(1)).generate(anyString());
    }

    private static final class InMemoryPromptCache implements PromptCachePort {

        private final Map<String, AiAnswer> store = new HashMap<>();

        @Override
        public Optional<AiAnswer> find(String hash) {
            return Optional.ofNullable(store.get(hash));
        }

        @Override
        public void put(String hash, AiAnswer answer) {
            store.put(hash, answer);
        }
    }
}
