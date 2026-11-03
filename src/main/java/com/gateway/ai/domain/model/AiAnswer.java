package com.gateway.ai.domain.model;

public record AiAnswer(String answer, String cacheType) {

    public static final String CACHE_TYPE_NONE = "NONE";
    public static final String CACHE_TYPE_EXACT = "EXACT_CACHE";
}
