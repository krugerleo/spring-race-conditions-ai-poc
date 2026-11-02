package com.gateway.ai.infrastructure.config;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.application.service.AiOrchestratorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
    @Bean
    public ProcessPromptUseCase processPromptUseCase(LlmServicePort llmServicePort) {
        return new AiOrchestratorService(llmServicePort);
    }
}
