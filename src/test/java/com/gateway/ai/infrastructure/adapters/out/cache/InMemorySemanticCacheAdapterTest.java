package com.gateway.ai.infrastructure.adapters.out.cache;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySemanticCacheAdapterTest {

    @Test
    void shouldReturnCachedAnswerWhenSimilarityIsAboveThreshold() {
        EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
        Mockito.when(embeddingModel.embed("pergunta original"))
                .thenReturn(Response.from(Embedding.from(new float[]{1f, 0f})));
        Mockito.when(embeddingModel.embed("pergunta parecida"))
                .thenReturn(Response.from(Embedding.from(new float[]{1f, 0f})));

        InMemorySemanticCacheAdapter adapter = new InMemorySemanticCacheAdapter(embeddingModel, 0.95);
        adapter.store("pergunta original", "resposta original");

        assertEquals("resposta original", adapter.findSimilar("pergunta parecida").orElseThrow());
    }

    @Test
    void shouldMissWhenSimilarityIsBelowThreshold() {
        EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
        Mockito.when(embeddingModel.embed("pergunta original"))
                .thenReturn(Response.from(Embedding.from(new float[]{1f, 0f})));
        Mockito.when(embeddingModel.embed("pergunta diferente"))
                .thenReturn(Response.from(Embedding.from(new float[]{0f, 1f})));

        InMemorySemanticCacheAdapter adapter = new InMemorySemanticCacheAdapter(embeddingModel, 0.95);
        adapter.store("pergunta original", "resposta original");

        assertTrue(adapter.findSimilar("pergunta diferente").isEmpty());
    }
}
