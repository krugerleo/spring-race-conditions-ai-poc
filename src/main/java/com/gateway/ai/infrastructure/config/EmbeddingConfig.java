package com.gateway.ai.infrastructure.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${LLM_API_KEY}") String apiKey,
            @Value("${ai.embedding.model:gemini-embedding-001}") String modelName) {

        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .taskType(GoogleAiEmbeddingModel.TaskType.SEMANTIC_SIMILARITY)
                .timeout(Duration.ofSeconds(15))
                .maxRetries(2)
                .build();
    }
}
