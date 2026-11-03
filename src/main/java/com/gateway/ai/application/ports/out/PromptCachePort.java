package com.gateway.ai.application.ports.out;

import com.gateway.ai.domain.model.AiAnswer;

import java.util.Optional;

public interface PromptCachePort {

    Optional<AiAnswer> find(String hash);

    void put(String hash, AiAnswer answer);
}
