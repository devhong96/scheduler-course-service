package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final MemberServiceClient memberServiceClient;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final CourseJpaRepository courseJpaRepository;

    private final ObjectMapper objectMapper;

    private final LocalDate localDate = LocalDate.now();

    @Override
    @Transactional
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackSaveClassTable")
    public void applyCourse(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        try {
            String value = objectMapper.writeValueAsString(
                    new CourseRequestMessage(studentInfo, upsertCourseRequest));

            kafkaTemplate.send("topic_course_schedule_logs", value);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void fallbackSaveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Reason: {}", e.getMessage());
        throw new RuntimeException("수업 정보를 저장할 수 없습니다. 다시 시도해 주세요.");
    }

    @KafkaListener(
            topics = "topic_course_schedule_logs",
            groupId = "group-1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void saveCourseTable(List<String> messages) {
        if (messages.isEmpty()) return;
        List<CourseSchedule> courseScheduleList = new ArrayList<>();

        int currentYear = localDate.getYear();
        int currentWeek = localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear());

        // Redis 캐시 키 구성: 예) "courseSchedules:2025:11"
        String cacheKey = "courseSchedules:" + currentYear + ":" + currentWeek;
        RBucket<List<CourseSchedule>> bucket = redissonClient.getBucket(cacheKey);

        List<CourseSchedule> redisScheduleList = bucket.get();

        if (redisScheduleList == null) {
            redisScheduleList = courseJpaRepository.findAllByCourseYearAndWeekOfYear(currentYear, currentWeek);
            bucket.set(redisScheduleList);
        }

        for (String message : messages) {
            try {
                CourseRequestMessage courseScheduleMessage = objectMapper.readValue(message, CourseRequestMessage.class);

                String lockKey = "courseLock:" + courseScheduleMessage.getStudentId();
                RLock lock = redissonClient.getLock(lockKey);

                boolean available = lock.tryLock(10, 1, SECONDS);

                if (!available) {
                    log.warn("Skipping processing for studentId {} as it's locked.", courseScheduleMessage.getStudentId());
                    continue;
                }

                try {
                    // 해당 시간대에 다른 학생의 수업이 있는지 확인
                    checkScheduleConflict(courseScheduleMessage, redisScheduleList);

                    // 기존 데이터 조회 (중복 방지)
                    Optional<CourseSchedule> existingSchedule = courseJpaRepository
                            .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear(
                                    courseScheduleMessage.getStudentId(),
                                    currentYear,
                                    currentWeek
                            );

                    if (existingSchedule.isPresent()) {
                        // 기존 데이터 업데이트
                        CourseSchedule courseSchedule = existingSchedule.get();
                        courseSchedule.updateSchedule(courseScheduleMessage);
                        courseJpaRepository.save(courseSchedule);
                        redisScheduleList.removeIf(s -> s.getStudentId().equals(courseScheduleMessage.getStudentId()));
                        redisScheduleList.add(courseSchedule);
                    } else {
                        // 새로운 데이터 추가
                        CourseSchedule courseSchedule = CourseSchedule.create(courseScheduleMessage);
                        courseScheduleList.add(courseSchedule);
                        redisScheduleList.add(courseSchedule);
                    }

                } finally {
                    lock.unlock();
                }

            } catch (Exception e) {
                log.error("Error processing message: {}", message, e);
                throw new RuntimeException(e);
            }
        }

        if (!courseScheduleList.isEmpty()) {
            courseJpaRepository.saveAll(courseScheduleList);
            bucket.set(redisScheduleList);
        }
    }

    private void checkScheduleConflict(CourseRequestMessage newSchedule, List<CourseSchedule> schedules) {
        for (CourseSchedule existing : schedules) {
            // 동일 학생의 스케줄은 체크하지 않음
            if (existing.getStudentId().equals(newSchedule.getStudentId())) {
                continue;
            }

            // 요일별 시간 비교 (null 안전 비교)
            if (Objects.equals(newSchedule.getMondayClassHour(), existing.getMondayClassHour()) ||
                    Objects.equals(newSchedule.getTuesdayClassHour(), existing.getTuesdayClassHour()) ||
                    Objects.equals(newSchedule.getWednesdayClassHour(), existing.getWednesdayClassHour()) ||
                    Objects.equals(newSchedule.getThursdayClassHour(), existing.getThursdayClassHour()) ||
                    Objects.equals(newSchedule.getFridayClassHour(), existing.getFridayClassHour())) {

                throw new RuntimeException(
                        String.format("Schedule conflict detected for teacher %s with existing student %s on same time slot",
                                newSchedule.getTeacherName(), existing.getStudentName())
                );
            }
        }
    }

    @Override
    @Transactional
    public void changeStudentName(ChangeStudentName changeStudentName) {

        String studentId = changeStudentName.getStudentId();

        CourseSchedule courseSchedule = courseJpaRepository
                .findCourseScheduleByStudentId(studentId)
                .orElseThrow(NoSuchElementException::new);

        courseSchedule.updateStudentName(changeStudentName.getStudentName());
    }

}