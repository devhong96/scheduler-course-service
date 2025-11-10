package com.scheduler.courseservice.outbox.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public boolean claim(String key) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent("idem:"+key, "1", Duration.ofDays(7));
        return Boolean.TRUE.equals(ok);
    }

}
