package com.gateway.ai.infrastructure.adapters.out.cache;

import com.gateway.ai.application.ports.out.SemanticCachePort;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySemanticCacheAdapter implements SemanticCachePort {

    private final EmbeddingModel embeddingModel;
    private final double minSimilarity;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public InMemorySemanticCacheAdapter(
            EmbeddingModel embeddingModel,
            @Value("${ai.semantic-cache.threshold:0.95}") double minSimilarity) {
        this.embeddingModel = embeddingModel;
        this.minSimilarity = minSimilarity;
    }

    @Override
    public Optional<String> findSimilar(String prompt) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        Embedding query = embed(prompt);
        Entry best = null;
        double bestScore = -1;

        for (Entry entry : entries.values()) {
            double score = CosineSimilarity.between(query, entry.embedding());
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }

        return best != null && bestScore >= minSimilarity
                ? Optional.of(best.answer())
                : Optional.empty();
    }

    @Override
    public void store(String prompt, String answer) {
        entries.put(prompt, new Entry(embed(prompt), answer));
    }

    private Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    private record Entry(Embedding embedding, String answer) {
    }
}
