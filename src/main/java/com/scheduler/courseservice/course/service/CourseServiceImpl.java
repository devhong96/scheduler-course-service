package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import com.scheduler.courseservice.outbox.service.CourseCreatedEventPayload;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import com.scheduler.courseservice.outbox.service.OutBoxEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeMemberNameDto;
import static com.scheduler.courseservice.outbox.domain.EventType.CREATED;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final DateProvider dateProvider;

    private final RedissonClient redissonClient;
    private final MemberServiceClient memberServiceClient;
    private final CourseJpaRepository courseJpaRepository;
    private final OutBoxEventPublisher outBoxEventPublisher;
    private final IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional("transactionManager")
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackSaveClassTable")
    public void applyCourse(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);
        outBoxEventPublisher.publish(
                CREATED, new CourseCreatedEventPayload(studentInfo, upsertCourseRequest)
        );
    }

    protected void fallbackSaveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Reason: ", e);
        throw new RuntimeException("수업 정보를 저장할 수 없습니다. 다시 시도해 주세요.");
    }

    @KafkaListener(
            topics = "${spring.kafka.topics.course.apply}",
            groupId = "${spring.kafka.topics.course.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void saveCourseTable(
            @Header(name = "Idempotency-Key", required = false) List<String> idemKeys,
            List<String> messages
    ) {
        if (messages.isEmpty()) return;

        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        // Redis 캐시 키 구성: 예) "courseSchedules:2025:11"
        String cacheKey = "courseSchedules:" + currentYear + ":" + currentWeek;
        RBucket<List<CourseSchedule>> bucket = redissonClient.getBucket(cacheKey);

        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            String idem = (idemKeys != null && idemKeys.size() > i) ? idemKeys.get(i) : null;

            try {

                if (idem != null && !idempotencyService.claim(idem)) {
                    continue; // 이미 처리된 메시지 → 스킵
                }

                CourseRequestMessage courseScheduleMessage = objectMapper.readValue(message, CourseRequestMessage.class);

                String lockKey = "courseLock:" + courseScheduleMessage.getTeacherId();
                RLock lock = redissonClient.getLock(lockKey);

                boolean available = lock.tryLock(5, SECONDS);

                if (!available) {
                    log.warn("Skipping processing for studentId {} as it's locked.", courseScheduleMessage.getStudentId());
                    continue;
                }

                try {

                    List<CourseSchedule> scheduleList = Optional.ofNullable(bucket.get())
                            .orElseGet(() -> courseJpaRepository.findAllByCourseYearAndWeekOfYear(currentYear, currentWeek));

                    // 해당 시간대에 다른 학생의 수업이 있는지 확인
                    checkScheduleConflict(courseScheduleMessage, scheduleList);

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
                        // 레코드 단위 즉시 저장 + 캐시 갱신
                        courseJpaRepository.save(courseSchedule);
                        scheduleList.removeIf(s -> s.getStudentId().equals(courseScheduleMessage.getStudentId()));
                        scheduleList.add(courseSchedule);
                    } else {
                        // 새로운 데이터 추가
                        CourseSchedule courseSchedule = CourseSchedule.create(courseScheduleMessage);
                        courseJpaRepository.save(courseSchedule);
                        scheduleList.add(courseSchedule);
                    }

                    bucket.set(new ArrayList<>(scheduleList));
                } finally {
                    lock.unlock();
                }

            } catch (Exception e) {
                log.error("Error processing message: {}", message, e);
                throw new DuplicateCourseException(e);
            }
        }
    }

    private void checkScheduleConflict(CourseRequestMessage newSchedule, List<CourseSchedule> schedules) {
        for (CourseSchedule existing : schedules) {
            if (!Objects.equals(existing.getTeacherId(), newSchedule.getTeacherId())) continue; // 같은 선생님만
            if (Objects.equals(existing.getStudentId(), newSchedule.getStudentId())) continue;  // 동일 학생은 패스

            // 요일별 시간 비교 (null 안전 비교)
            if (Objects.equals(newSchedule.getMondayClassHour(), existing.getMondayClassHour())
                    || Objects.equals(newSchedule.getTuesdayClassHour(), existing.getTuesdayClassHour())
                    || Objects.equals(newSchedule.getWednesdayClassHour(), existing.getWednesdayClassHour())
                    || Objects.equals(newSchedule.getThursdayClassHour(), existing.getThursdayClassHour())
                    || Objects.equals(newSchedule.getFridayClassHour(), existing.getFridayClassHour())) {
                throw new DuplicateCourseException(
                        String.format("Schedule conflict detected for teacher %s with existing student %s on same time slot",
                                newSchedule.getTeacherName(), existing.getStudentName())
                );
            }
        }
    }


    @Override
    @Transactional
    public void changeStudentName(ChangeMemberNameDto changeMemberNameDto) {

        String studentId = changeMemberNameDto.getMemberId();

        CourseSchedule courseSchedule = courseJpaRepository.findCourseScheduleByStudentId(studentId)
                .orElseThrow(NoSuchElementException::new);

        courseSchedule.updateStudentName(changeMemberNameDto.getNewName());
    }

}