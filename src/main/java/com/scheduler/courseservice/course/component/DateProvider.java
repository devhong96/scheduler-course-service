package com.scheduler.courseservice.course.component;

import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

@Getter
@Component
public class DateProvider {

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
        this.currentWeek = today.get(WeekFields.ISO.weekOfYear());
    }
}
