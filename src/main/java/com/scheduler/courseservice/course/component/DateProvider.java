package com.scheduler.courseservice.course.component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class DateProvider {

    private final RedissonClient redissonClient;

    private int currentYear;
    private int currentWeek;

    @PostConstruct
    public void init() {
        updateDate(); // 서버 시작 시 한 번 실행
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void updateDate() {
        LocalDate today = LocalDate.now();
        this.currentYear = today.getYear();
        this.currentWeek = today.get(WeekFields.of(Locale.getDefault()).weekOfYear());
    }
}
