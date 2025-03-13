package com.scheduler.courseservice.course.component;

import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static java.time.temporal.WeekFields.ISO;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class DateProvider {

    private final RedissonClient redissonClient;

    private int currentYear;
    private int currentWeek;

    @PrePersist
    public void init() {
        updateDate(); // 서버 시작 시 한 번 실행
    }

    @Scheduled(cron = "0 0 0 * * MON") //
    public void updateDate() {
        LocalDate today = LocalDate.now();
        this.currentYear = today.getYear();
        this.currentWeek = today.get(ISO.weekOfYear());
    }

    @Scheduled(cron = "58 59 23 ? * SUN")
    public void clearPreviousWeekSchedules() {
        redissonClient.getKeys().flushdb();
        log.info("All Redis data has been flushed.");
    }
}
