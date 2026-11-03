package com.gateway.ai.infrastructure.config;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.ports.out.PromptCachePort;
import com.gateway.ai.application.service.AiGenerationService;
import com.gateway.ai.application.service.AiOrchestratorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public AiGenerationService aiGenerationService(LlmServicePort llmServicePort,
                                                   PromptCachePort promptCachePort) {
        return new AiGenerationService(llmServicePort, promptCachePort);
    }

    @Bean
    public ProcessPromptUseCase processPromptUseCase(AiGenerationService aiGenerationService,
                                                     PromptCachePort promptCachePort) {
        return new AiOrchestratorService(aiGenerationService, promptCachePort);
    }
}
