package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.mockWeek;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.mockYear;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@IntegrationTest
class CourseScheduleServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @MockitoBean
    @Qualifier("testKafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("통합 동시성 테스트")
    void saveCourseTable_race_condition() {

        UpsertCourseRequest req = new UpsertCourseRequest();
        req.setMondayClassHour(1);
        req.setTuesdayClassHour(1);
        req.setWednesdayClassHour(1);
        req.setThursdayClassHour(1);
        req.setFridayClassHour(1);

        // 서로 다른 키로 가도록 토큰/키 매핑을 서비스가 지원해야 함(테스트 더블/설정으로)
        Runnable t1 = () -> courseService.applyCourse("token_student_1", req);
        Runnable t2 = () -> courseService.applyCourse("token_student_2", req);

        ExecutorService exec = Executors.newFixedThreadPool(2);

        try {
            exec.submit(t1); exec.submit(t2);
        } finally { exec.shutdown(); }

        int year = LocalDate.now().getYear();
        int week = LocalDate.now().get(WeekFields.ISO.weekOfYear());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<CourseSchedule> all = courseJpaRepository.findAllByCourseYearAndWeekOfYear(year, week);
            assertThat(all).hasSize(1);
        });
    }

    @Test
    @DisplayName("consumer concurrency: 같은 선생님/같은 시간대 동시 신청 시 한 건만 저장")
    void raceCondition() throws Exception {
        // given: 같은 시간대
        UpsertCourseRequest req = new UpsertCourseRequest();
        req.setMondayClassHour(1);
        req.setTuesdayClassHour(1);
        req.setWednesdayClassHour(1);
        req.setThursdayClassHour(1);
        req.setFridayClassHour(1);

        // 같은 선생님, 다른 학생
        CourseRequestMessage m1 = new CourseRequestMessage(
                new StudentInfo("teacher_001", "Mr. Kim", "student1", "student1"), req);
        CourseRequestMessage m2 = new CourseRequestMessage(
                new StudentInfo("teacher_001", "Mr. Kim", "student2", "student2"), req);

        String json1 = objectMapper.writeValueAsString(m1);
        String json2 = objectMapper.writeValueAsString(m2);

        // when: 서로 다른 파티션(0, 1)에 동시에 전송 → 컨슈머 스레드 2개가 병렬 처리
        kafkaTemplate.send("course_schedule_logs", 0, UUID.randomUUID().toString(), json1);
        kafkaTemplate.send("course_schedule_logs", 1, UUID.randomUUID().toString(), json2);

        int year = LocalDate.now().getYear();
        int week = LocalDate.now().get(WeekFields.ISO.weekOfYear());

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<CourseSchedule> all = courseJpaRepository.findAllByCourseYearAndWeekOfYear(year, week);
                    assertThat(all).hasSize(1);
                    assertThat(all.get(0).getTeacherId()).isEqualTo("teacher_001");
                });
    }

    @Test
    @DisplayName("같은 멱등 키 테스트. 두 번 → 한 번만 처리")
    void idempotency_blocks_duplicate() throws Exception {

        Acknowledgment mockAck = mock(Acknowledgment.class);
        UpsertCourseRequest request = new UpsertCourseRequest();

        request.setMondayClassHour(1);
        request.setTuesdayClassHour(1);
        request.setWednesdayClassHour(1);
        request.setThursdayClassHour(1);
        request.setFridayClassHour(1);

        String json = objectMapper.writeValueAsString(new CourseRequestMessage(
                new StudentInfo("teacher_001","Mr. Kim","student_009","Irene Seo"), request));

        String sameIdem = UUID.randomUUID().toString();
        when(idempotencyService.claim(sameIdem)).thenReturn(true, false);

        courseService.saveCourseTable(List.of(sameIdem), List.of(json), mockAck);
        courseService.saveCourseTable(List.of(sameIdem), List.of(json), mockAck);

        // 기대: 1건만 존재
        long count = courseJpaRepository
                .findAllByCourseYearAndWeekOfYear(mockYear, mockWeek)
                .stream().filter(s -> "teacher_001".equals(s.getTeacherId())).count();

        assertThat(count).isEqualTo(1);
    }
}