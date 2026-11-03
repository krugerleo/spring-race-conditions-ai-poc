package com.gateway.ai;

import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
import com.gateway.ai.application.ports.out.LlmServicePort;
import com.gateway.ai.domain.model.AiAnswer;
import com.gateway.ai.domain.model.PromptQuery;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(properties = "LLM_API_KEY=test")
class ConcurrencyWithRedisTestcontainersTest {

    private static final int THREADS = 50;

    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @MockitoBean
    private LlmServicePort llmServicePort;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Autowired
    private ProcessPromptUseCase processPromptUseCase;

    @Test
    void shouldCallLlmExactlyOnceUnderHeavyConcurrency() throws Exception {
        Mockito.when(llmServicePort.generate(anyString())).thenReturn("Resposta simulada da IA");
        Mockito.when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{1f, 0f})));

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AiAnswer>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return processPromptUseCase.process(new PromptQuery("Como funciona lock distribuído?"));
            }));
        }

        start.countDown();
        executor.shutdown();

        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Timeout no teste de concorrência");

        for (Future<AiAnswer> future : futures) {
            assertEquals("Resposta simulada da IA", future.get(1, TimeUnit.SECONDS).answer());
        }

        Mockito.verify(llmServicePort, Mockito.times(1)).generate(anyString());
    }
}
