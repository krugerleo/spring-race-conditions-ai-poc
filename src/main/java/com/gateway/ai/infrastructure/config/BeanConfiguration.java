package com.gateway.ai.infrastructure.config;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.SemanticCachePort;
import com.gateway.ai.application.service.AiOrchestratorService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public ProcessPromptUseCase processPromptUseCase(LlmServicePort llmServicePort,
                                                     SemanticCachePort semanticCachePort,
                                                     CacheManager cacheManager,
                                                     MeterRegistry meterRegistry) {
        return new AiOrchestratorService(llmServicePort, semanticCachePort, cacheManager, meterRegistry);
    }
}
