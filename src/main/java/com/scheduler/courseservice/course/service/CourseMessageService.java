package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.dto.CourseInfoRequest;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMessageService {

    private final RedissonClient redissonClient;
    private final CourseJpaRepository courseJpaRepository;
    private final IdempotencyService idempotencyService;
    private final DateProvider dateProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processMessage(String idem, String message) throws Exception {
        if (idem != null && !idempotencyService.claim(idem)) {
            return; // 이미 처리된 메시지 → 스킵
        }

        CourseInfoRequest.CourseRequestMessage courseMessage = objectMapper.readValue(message, CourseInfoRequest.CourseRequestMessage.class);

        String lockKey = "courseLock:" + courseMessage.getTeacherId();
        RLock lock = redissonClient.getLock(lockKey);

        boolean available = lock.tryLock(5, SECONDS);

        if (!available) {
            log.warn("Skipping processing for studentId {} as it's locked.", courseMessage.getStudentId());
            return;
        }

        try {
            updateSchedule(courseMessage);
        } finally {
            lock.unlock();
        }
    }

    private void updateSchedule(CourseInfoRequest.CourseRequestMessage courseMessage) {
        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        String teacherCacheKey = String.format("courseSchedules:%d:%d:teacher:%s",
                currentYear,
                currentWeek,
                courseMessage.getTeacherId());

        RBucket<List<CourseSchedule>> teacherBucket = redissonClient.getBucket(teacherCacheKey);

        List<CourseSchedule> scheduleList = Optional.ofNullable(teacherBucket.get())
                .orElseGet(() -> courseJpaRepository.findAllCourseScheduleByTeacherIdAndCourseYearAndWeekOfYear(
                        courseMessage.getTeacherId(),
                        currentYear,
                        currentWeek
                ));

        // 해당 시간대에 다른 학생의 수업이 있는지 확인
        checkScheduleConflict(courseMessage, scheduleList);

        // 기존 데이터 조회 (중복 방지)
        Optional<CourseSchedule> existingSchedule = scheduleList.stream()
                .filter(s -> s.getStudentId().equals(courseMessage.getStudentId()))
                .findFirst();

        if (existingSchedule.isPresent()) {
            // 기존 데이터 업데이트
            CourseSchedule courseSchedule = existingSchedule.get();
            courseSchedule.updateSchedule(courseMessage);
            // 레코드 단위 즉시 저장 + 캐시 갱신
            courseJpaRepository.save(courseSchedule);
            scheduleList.removeIf(s -> s.getStudentId().equals(courseMessage.getStudentId()));
            scheduleList.add(courseSchedule);
        } else {
            // 새로운 데이터 추가
            CourseSchedule courseSchedule = CourseSchedule.create(courseMessage);
            courseJpaRepository.save(courseSchedule);
            scheduleList.add(courseSchedule);
        }

        teacherBucket.set(new ArrayList<>(scheduleList));
    }

    private void checkScheduleConflict(CourseInfoRequest.CourseRequestMessage newSchedule, List<CourseSchedule> schedules) {
        for (CourseSchedule existing : schedules) {

            if (Objects.equals(existing.getStudentId(), newSchedule.getStudentId())) continue;

            checkSlot(newSchedule.getMondayClassHour(), existing.getMondayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getTuesdayClassHour(), existing.getTuesdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getWednesdayClassHour(), existing.getWednesdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getThursdayClassHour(), existing.getThursdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getFridayClassHour(), existing.getFridayClassHour(), newSchedule, existing);
        }
    }

    private void checkSlot(Integer newHour, Integer existingHour, CourseInfoRequest.CourseRequestMessage newSchedule, CourseSchedule existing) {
        boolean isNewSlotValid = (newHour != null && newHour != 0);

        if (isNewSlotValid && Objects.equals(newHour, existingHour)) {
            throw new DuplicateCourseException(
                    String.format("Schedule conflict detected for teacher %s with existing student %s on same time slot",
                            newSchedule.getTeacherName(), existing.getStudentName())
            );
        }
    }
}
