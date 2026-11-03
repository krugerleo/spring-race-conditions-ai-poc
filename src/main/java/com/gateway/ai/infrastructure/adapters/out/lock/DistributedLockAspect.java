package com.gateway.ai.infrastructure.adapters.out.lock;

import com.gateway.ai.domain.model.PromptQuery;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    public DistributedLockAspect(RedissonClient redissonClient, MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String hash = (args.length > 0 && args[0] instanceof PromptQuery query)
                ? query.getHash()
                : String.valueOf(args[0].hashCode());

        String lockKey = "lock:prompt:" + hash;
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS);
        if (!acquired) {
            meterRegistry.counter("ai.locks.timeouts").increment();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Requisição duplicada em processamento. O sistema está protegendo a cota da LLM.");
        }
        meterRegistry.counter("ai.locks.acquired").increment();

        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}