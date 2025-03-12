package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final MemberServiceClient memberServiceClient;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    private final ObjectMapper objectMapper;

    private final LocalDate localDate = LocalDate.now();

    @Override
    @Transactional
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackSaveClassTable")
    public void applyCourse(
            String token, UpsertCourseRequest upsertCourseRequest
    ) {

        duplicateClassValidator(upsertCourseRequest, null);

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        String key = "courseLock:" + studentInfo.getStudentId();
        RLock lock = redissonClient.getLock(key);

        try {
            boolean available = lock.tryLock(10, 1, SECONDS);

            if (available) {
                try {
                    String value = objectMapper.writeValueAsString(
                            new CourseRequestMessage(studentInfo, upsertCourseRequest));

                    kafkaTemplate.send("topic_course_schedule_logs", value);

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Timeout: Unable to acquire lock");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackModifyClassTable")
    public void modifyCourse(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        CourseSchedule existingCourse = courseJpaRepository
                .findCourseScheduleByStudentId((studentInfo.getStudentId()))
                .orElseThrow(() -> new IllegalStateException("기존 수업을 찾을 수 없습니다."));

        duplicateClassValidator(upsertCourseRequest, existingCourse);

        existingCourse.updateSchedule(upsertCourseRequest);
    }

    protected void fallbackSaveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Reason: {}", e.getMessage());

        throw new RuntimeException("수업 정보를 저장할 수 없습니다. 다시 시도해 주세요.");
    }

    private void duplicateClassValidator(UpsertCourseRequest upsertCourseRequest, CourseSchedule existingCourse) {

        List<StudentCourseResponse> studentCourseList = courseRepository
                .getAllStudentsWeeklyCoursesForComparison(
                        localDate.getYear(), localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear()));

        for (StudentCourseResponse studentCourseResponse : studentCourseList) {

            if (existingCourse != null &&
                    studentCourseResponse.getStudentId().equals(existingCourse.getStudentId())) {
                continue;
            }

            if (isOverlapping(studentCourseResponse, upsertCourseRequest)) {
                throw new DuplicateCourseException("수업이 중복됩니다.");
            }
        }
    }

    private boolean isOverlapping(StudentCourseResponse existing, UpsertCourseRequest newClass) {
        return Objects.equals(existing.getMondayClassHour(), newClass.getMondayClassHour()) ||
                Objects.equals(existing.getTuesdayClassHour(), newClass.getTuesdayClassHour()) ||
                Objects.equals(existing.getWednesdayClassHour(), newClass.getWednesdayClassHour()) ||
                Objects.equals(existing.getThursdayClassHour(), newClass.getThursdayClassHour()) ||
                Objects.equals(existing.getFridayClassHour(), newClass.getFridayClassHour());
    }

    protected void fallbackModifyClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Fallback activated for modifyClassTable. Reason: {}", e.getMessage());

        throw new RuntimeException("수업 정보를 수정할 수 없습니다. 다시 시도해 주세요.");
    }

    @KafkaListener(
            topics = "topic_course_schedule_logs",
            groupId = "group-1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void saveClassTable(List<String> messages) {
        if (messages.isEmpty()) return;

        List<CourseSchedule> courseSchedules = new ArrayList<>();

        for (String message : messages) {

            try {

                CourseRequestMessage courseSchedule = objectMapper.readValue(message, CourseRequestMessage.class);

                courseSchedules.add(CourseSchedule.create(courseSchedule));

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        courseJpaRepository.saveAll(courseSchedules);
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