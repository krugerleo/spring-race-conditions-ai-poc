package com.gateway.ai.infrastructure.adapters.out.cache;

import com.gateway.ai.application.ports.out.PromptCachePort;
import com.gateway.ai.domain.model.AiAnswer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RedisPromptCacheAdapter implements PromptCachePort {

    private static final String CACHE_NAME = "exact-cache";

    private final CacheManager cacheManager;

    public RedisPromptCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<AiAnswer> find(String hash) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(hash, AiAnswer.class));
    }

    @Override
    public void put(String hash, AiAnswer answer) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.put(hash, answer);
        }
    }
}
