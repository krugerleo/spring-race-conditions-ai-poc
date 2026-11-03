# Race for AI

This is a small proof of concept I built to explore a problem that shows up as soon as you put an LLM behind a public API: **what happens when a lot of people send the same prompt at the same time?**

If nothing protects the endpoint, every request goes straight to the model. You pay for the same answer dozens of times, burn through your quota, and add latency for everyone. This project tries to fix that without turning the application into a distributed systems monster.

The short version:

- A Redis lock makes sure only one request per prompt reaches the LLM.
- A normal cache handles repeated prompts.
- A semantic cache handles prompts that mean the same thing but use different words.
- Metrics show what happened.
- A test with 50 concurrent threads proves the main point.

I kept the code intentionally small. It still follows the hexagonal structure, but there are only a few classes and one orchestrator.

---

## The problem

Imagine 50 clients sending this at the same time:

```text
"Explain distributed locks in one sentence."
```

All 50 check the cache, all 50 see nothing, and all 50 call the LLM. That is the classic thundering herd problem, only now the stampede is expensive.

Even without concurrency, users rarely type the exact same prompt twice:

```text
"Explain distributed locks"
"Can you explain distributed locking?"
"What is a distributed lock?"
```

A regular key-value cache sees three different keys and calls the model three times. That is the second problem this POC tries to solve.

---

## How it works

A request goes through this path:

```text
POST /api/v1/ai/generate
        |
        v
  Redisson lock (one request per prompt)
        |
        v
  exact cache in Redis
        |
        v
  semantic cache (embeddings + cosine similarity)
        |
        v
  LLM call, then store the answer in both caches
```

### 1. The lock

`AiOrchestratorService.process()` is annotated with a custom `@DistributedLock`. A small AOP aspect acquires a Redisson lock using the prompt hash as part of the key.

```java
@DistributedLock(key = "#query.hash", waitTime = 5, leaseTime = 10)
public AiAnswer process(PromptQuery query) {
    // exact cache -> semantic cache -> LLM
}
```

The first request gets the lock and does the work. The others wait. When they get the lock, the answer is already in Redis, so they return from the exact cache. If someone waits more than five seconds, the API returns `429` instead of piling more duplicate calls onto the provider.

### 2. The exact cache

This is a regular Spring `CacheManager` backed by Redis:

- cache name: `exact-cache`
- TTL: 10 minutes
- keys and values stored as plain strings

The cache is checked inside the lock, not before it. That is the important part. If the cache were checked before the lock, two requests could both miss and both call the LLM. Checking after the lock means the second request always sees what the first one stored.

### 3. The semantic cache

This is where the AI part gets more interesting.

The prompt is converted into an embedding. That embedding is compared with the embeddings of prompts that were answered before. If the cosine similarity is at least `0.95`, the stored answer is returned.

```text
prompt -> embedding -> compare with stored vectors
                          |
                          +-- score >= 0.95 -> cached answer
                          +-- otherwise     -> call the LLM
```

The semantic cache is behind a `SemanticCachePort`, so it can be replaced later without touching the application service. Right now the implementation is in memory, which is fine for a POC and not fine for production. More on that below.

### 4. The response header

I did not want the cache source inside the response body, so it goes in a header:

```http
HTTP/1.1 200 OK
X-Cache-Type: SEMANTIC_HIT

{"answer":"..."}
```

The possible values are `EXACT_HIT`, `SEMANTIC_HIT`, and `MISS`.

---

## The AI side

AI is the reason this POC exists, not just a detail in it.

The chat model is Gemini through LangChain4j, configured in `LangChain4jAdapter`:

- model: `gemini-3.1-flash-lite`
- API key: `LLM_API_KEY`

The embedding model is also Gemini:

- model: `gemini-embedding-001`
- task type: `SEMANTIC_SIMILARITY`
- threshold: `0.95`

An embedding turns text into a vector. Texts with similar meaning end up close together, even when the words are different. That is how the gateway can treat "Explain distributed locks" and "Can you explain distributed locking?" as the same question.

I chose a strict threshold on purpose. A lower value means more cache hits, but also a higher chance of returning an answer to a question the user did not ask. `0.95` felt like a reasonable starting point for a POC, and it is configurable.

One thing worth saying clearly: a semantic hit avoids the **chat** call, but it still generates an embedding to do the lookup. Embeddings are much cheaper than chat completions, but they are not free. And on a full miss, the prompt is embedded once for the lookup and again when it is stored. That is a trade-off I accepted to keep the code simple.

---

## Project layout

The structure is hexagonal, but with very little ceremony:

```text
src/main/java/com/gateway/ai/
├── application/
│   ├── ports/in/ProcessPromptUseCase.java
│   ├── ports/out/LlmServicePort.java
│   ├── ports/out/SemanticCachePort.java
│   └── service/AiOrchestratorService.java
├── domain/model/
│   ├── AiAnswer.java
│   ├── CacheType.java
│   └── PromptQuery.java
└── infrastructure/
    ├── adapters/in/PromptController.java
    ├── adapters/out/cache/InMemorySemanticCacheAdapter.java
    ├── adapters/out/llm/LangChain4jAdapter.java
    ├── adapters/out/lock/DistributedLock.java
    ├── adapters/out/lock/DistributedLockAspect.java
    └── config/
        ├── BeanConfiguration.java
        ├── CacheConfig.java
        └── EmbeddingConfig.java
```

The application layer knows about interfaces. Redis, Gemini, and Spring MVC live in the infrastructure layer.

---

## Stack

- Java 21
- Spring Boot 4.1.2
- Spring Data Redis for the exact cache
- Redisson for the distributed lock
- Spring AOP for the lock aspect
- LangChain4j with Gemini for chat and embeddings
- Micrometer and Actuator for metrics
- Testcontainers for the concurrency test
- Docker Compose for local Redis

---

## Running it

You need JDK 21, Maven, Docker, and a Gemini API key.

Create a `.env` file in the project root:

```properties
LLM_API_KEY=your-key-here
```

Start Redis:

```bash
docker compose -f Docker/docker-compose.yml up -d
```

Run the application:

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

A few settings live in `application.properties`:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379

ai.embedding.model=gemini-embedding-001
ai.semantic-cache.threshold=0.95
```

If you ran an older version of this project, flush the old Redis cache before testing. Values used to be stored as JSON and are now plain strings:

```bash
docker exec -it redis-ai-gateway redis-cli FLUSHALL
```

---

## Trying it

First request:

```bash
curl -i -X POST http://localhost:8080/api/v1/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Explain distributed locks in one sentence."}'
```

You should see:

```http
X-Cache-Type: MISS
```

Send the same thing again and you should get:

```http
X-Cache-Type: EXACT_HIT
```

Now try the same idea with different words:

```bash
curl -i -X POST http://localhost:8080/api/v1/ai/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Can you explain distributed locking in one sentence?"}'
```

If the similarity passes the threshold:

```http
X-Cache-Type: SEMANTIC_HIT
```

The file `src/main/resources/Requests.http` has these three scenarios ready to run from IntelliJ.

If a duplicate request cannot get the lock within five seconds, it gets a `429`. That is deliberate. I would rather reject the duplicate than let it consume more provider quota.

---

## Metrics

Actuator exposes the usual endpoints:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

The counters I added:

| Metric | What it means |
| --- | --- |
| `ai.calls.total` | How many times the LLM was actually called |
| `ai.cache.exact.hits` | Requests answered by the Redis cache |
| `ai.cache.semantic.hits` | Requests answered by the semantic cache |
| `ai.locks.acquired` | Locks acquired successfully |
| `ai.locks.timeouts` | Requests that gave up waiting for the lock |

For example:

```bash
curl http://localhost:8080/actuator/metrics/ai.calls.total
```

---

## Tests

```bash
mvn test
```

Current result:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

There are unit tests for the exact cache, the semantic cache hit, and the threshold miss. The one I care about most is the integration test.

It starts a real Redis container with Testcontainers, launches 50 threads with a `CountDownLatch`, and sends the same prompt from all of them:

```java
Mockito.verify(llmServicePort, Mockito.times(1)).generate(anyString());
```

Fifty concurrent requests, one LLM call. The LLM and embedding model are mocked, so the test does not spend real API quota. Redis is real because the lock and the exact cache are the parts being tested.

Docker needs to be running for this test.

---

## What is still rough

This is a POC, so there are some obvious gaps.

**The semantic cache is in memory.** It works in one instance, disappears on restart, is not shared between replicas, and grows without eviction. For production I would replace it with RedisSearch, pgvector, or Qdrant. The port makes that change straightforward.

**Semantic search is O(n).** Every lookup compares the query with every stored vector. Fine for hundreds of entries, not fine for a real workload.

**Concurrent paraphrases can still duplicate calls.** The lock key comes from the exact prompt hash. Two different wordings arriving at the same time have different keys, so both can miss the semantic cache and call the LLM. Fixing that needs some kind of semantic or coarser lock, which would add contention and complexity. I left it out on purpose.

**The hash is not collision-resistant.** `PromptQuery` uses `Integer.toHexString(prompt.hashCode())`. For production I would use SHA-256 and include the model or prompt-template version in the key.

**The lock lease is 10 seconds.** If the LLM takes longer than that, the lock can expire while the first request is still working. A watchdog or a longer lease would be needed in production. The five-second wait time has the same tuning problem: a slow but valid first request can cause duplicates to receive `429`.

**There is no cache invalidation.** Answers stay until the Redis TTL expires or the process restarts. If the model or prompt template changes, old answers remain. A cache version in the key would help.

**There is no auth, per-user rate limit, retry, fallback, or circuit breaker.** The endpoint is open, and a provider failure is just a failure. Those are production concerns, not POC concerns, but they are real.

**Embeddings cost money too.** A semantic hit is cheaper than a chat completion, not free. And a miss embeds the prompt twice.

---

## What I would do next

If I kept working on this, the order would probably be:

1. Move the semantic cache to a shared vector store.
2. Handle paraphrased duplicates that arrive at the same time.
3. Use SHA-256 with a cache version.
4. Tune the lock timings against real LLM latency, or add a watchdog.
5. Add Prometheus and Grafana to see the counters over time.
6. Add retries, timeouts, and a circuit breaker around the provider.
7. Add authentication and per-client rate limiting.

None of that is necessary to understand the idea. The POC already shows the part that matters: a burst of identical prompts should not turn into a burst of identical LLM calls.

---

## Final thought

The most interesting part of this project was not the lock itself. It was realizing that AI caching has two different definitions of "the same request". One is exact, and one is semantic, and they need different tools.

The lock and the exact cache are ordinary backend engineering. The semantic cache is where the AI-specific thinking shows up, and it is also the part I would expect to change the most in a real system.
