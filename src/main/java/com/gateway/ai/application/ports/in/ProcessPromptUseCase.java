package com.gateway.ai.application.ports.in;

import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;

public interface ProcessPromptUseCase {
    AiAnswer process(PromptQuery promptQuery);
}
