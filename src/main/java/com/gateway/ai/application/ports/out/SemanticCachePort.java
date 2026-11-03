package com.gateway.ai.application.ports.out;

import java.util.Optional;

public interface SemanticCachePort {

    Optional<String> findSimilar(String prompt);

    void store(String prompt, String answer);
}
