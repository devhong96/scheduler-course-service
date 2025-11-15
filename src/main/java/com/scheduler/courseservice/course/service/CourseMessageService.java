package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMessageService {

    private final CourseScheduleService courseScheduleService;
    private final RedissonClient redissonClient;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public void processMessage(String idem, String message) throws Exception {
        if (idem != null && !idempotencyService.claim(idem)) {
            return; // 이미 처리된 메시지는 스킵
        }

        CourseRequestMessage courseMessage = objectMapper.readValue(message, CourseRequestMessage.class);

        String lockKey = "courseLock:" + courseMessage.getTeacherId();
        RLock lock = redissonClient.getLock(lockKey);

        boolean available = lock.tryLock(5, SECONDS);

        if (!available) {
            log.warn("Skipping processing for studentId {} as it's locked.", courseMessage.getStudentId());
            throw new RuntimeException("Lock acquisition failed for key: " + lockKey);
        }

        try {
            courseScheduleService.updateSchedule(courseMessage);
        } finally {
            lock.unlock();
        }
    }
}
