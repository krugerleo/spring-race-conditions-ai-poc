//package com.gateway.ai;
//
//import com.gateway.ai.application.ports.out.LlmServicePort;
//import com.gateway.ai.application.ports.in.ProcessPromptUseCase;
//import com.gateway.ai.domain.model.PromptQuery;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest
//@Testcontainers
//class ConcurrencyWithRedisTestcontainersTest {
//
//    // Sobe um container Redis limpo exclusivamente para o teste
//    @Container
//    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
//            .withExposedPorts(6379);
//
//    // Configura dinamicamente a porta do Redis do Testcontainers para o Spring Boot
//    static {
//        System.setProperty("spring.data.redis.host", redisContainer.getHost());
//        System.setProperty("spring.data.redis.port", redisContainer.getMappedPort(6379).toString());
//    }
//
//    @Autowired
//    private ProcessPromptUseCase processPromptUseCase;
//
//    // Mockamos a saída da LLM para NÃO gastar tokens nem chamar a API real
//    @MockBean
//    private LlmServicePort llmServicePort;
//
//    @BeforeEach
//    void setUp() {
//        // Simula uma resposta instantânea da IA
//        Mockito.when(llmServicePort.generate(anyString()))
//                .thenReturn("Resposta simulada da IA sem gastar tokens.");
//    }
//
//    @Test
//    void shouldHandleThunderingHerdUnderHeavyConcurrency() throws InterruptedException {
//        int threadCount = 30; // 30 threads simultâneas
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(1);
//
//        String promptText = "Como funciona o Lock Distribuído?";
//
//        for (int i = 0; i < threadCount; i++) {
//            executor.execute(() -> {
//                try {
//                    latch.await(); // Aguarda o tiro de largada
//                    PromptQuery query = new PromptQuery(promptText);
//                    processPromptUseCase.process(query);
//                } catch (Exception ignored) {
//                }
//            });
//        }
//
//        // Dispara todas as 30 threads no mesmíssimo milissegundo (Simula Race Condition / Thundering Herd)
//        latch.countDown();
//
//        executor.shutdown();
//        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
//
//        assertDoesNotThrow(() -> {
//            if (!finished) throw new RuntimeException("Timeout no teste de concorrência.");
//        });
//
//        // 💡 A PROVA DE OURO DA POC:
//        // Mesmo com 30 threads concorrendo ao mesmo tempo, a LLM real DEVE ser chamada EXATAMENTE 1 ÚNICA VEZ.
//        // As outras 29 requisições pegaram o Lock ou o Cache Exato do Redis!
//        verify(llmServicePort, times(1)).generate(anyString());
//    }
//}