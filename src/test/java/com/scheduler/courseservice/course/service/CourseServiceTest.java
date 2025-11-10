package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.outbox.domain.EventType;
import com.scheduler.courseservice.outbox.service.CourseCreatedEventPayload;
import com.scheduler.courseservice.outbox.service.EventPayload;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import com.scheduler.courseservice.outbox.service.OutBoxEventPublisher;
import com.scheduler.courseservice.testSet.IntegrationTest;
import com.scheduler.courseservice.testSet.messaging.ChangeStudentNameRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.*;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private OutBoxEventPublisher outBoxEventPublisher;

    @MockitoBean
    @Qualifier("testKafkaTemplate")
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoSpyBean(name = "testKafkaListenerContainerFactory")
    private ConcurrentKafkaListenerContainerFactory<String,String> factory;

    @MockitoBean
    private MemberServiceClient memberServiceClient;

    @Autowired
    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMockServer() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }


    @AfterEach
    void stopWireMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }


    @Test
    @DisplayName("feign 확인")
    void feignStudentInfo() {

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");

        when(memberServiceClient.findStudentInfoByToken(token)).thenReturn(studentInfo);

        StudentInfo result = memberServiceClient.findStudentInfoByToken(token);

        assertThat(result)
                .isNotNull()
                .extracting(
                        StudentInfo::getTeacherId, StudentInfo::getTeacherName,
                        StudentInfo::getStudentId, StudentInfo::getStudentName
                )
                .containsExactly(
                        "teacher_001", "Mr. Kim",
                        "student_009", "Irene Seo"
                );
    }

    @Test
    @DisplayName("수업 전달")
    void applyCourse() {

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");
        when(memberServiceClient.findStudentInfoByToken(token)).thenReturn(studentInfo);

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(4);
        upsertCourseRequest.setWednesdayClassHour(3);
        upsertCourseRequest.setThursdayClassHour(2);
        upsertCourseRequest.setFridayClassHour(5);

        courseService.applyCourse(token, upsertCourseRequest);

        // Assert (검증)
        // 1. ArgumentCaptor를 사용하여 publish 메서드에 전달된 인자 캡처
        ArgumentCaptor<EventPayload> payloadCaptor = ArgumentCaptor.forClass(EventPayload.class);

        // 2. outBoxEventPublisher.publish가 1번 호출되었는지,
        //    첫 번째 인자가 EventType.CREATED인지,
        //    두 번째 인자(EventPayload)를 캡처
        verify(outBoxEventPublisher, times(1)).publish(
                eq(EventType.CREATED),
                payloadCaptor.capture()
        );

        // 3. 캡처된 페이로드 검증
        EventPayload capturedPayload = payloadCaptor.getValue();

        // 3-1. 페이로드가 올바른 타입인지 확인
        assertThat(capturedPayload).isInstanceOf(CourseCreatedEventPayload.class);

        // 3-2. 페이로드 내부의 데이터가 예상과 일치하는지 확인
        CourseCreatedEventPayload coursePayload = (CourseCreatedEventPayload) capturedPayload;
        assertThat(coursePayload.getTeacherId()).isEqualTo(studentInfo.getTeacherId());
        assertThat(coursePayload.getMondayClassHour()).isEqualTo(upsertCourseRequest.getMondayClassHour());

    }

    @Test
    @DisplayName("수업 수정")
    void saveCourseTable() throws JsonProcessingException {

        StudentInfo studentInfo = new StudentInfo(
                "teacher_001", "Mr. Kim",
                "student_009", "Irene Seo"
        );

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(2);
        upsertCourseRequest.setWednesdayClassHour(0);
        upsertCourseRequest.setThursdayClassHour(0);
        upsertCourseRequest.setFridayClassHour(0);

        CourseRequestMessage message = new CourseRequestMessage(studentInfo, upsertCourseRequest);
        String json = objectMapper.writeValueAsString(message);

        String idem = UUID.randomUUID().toString();

        when(idempotencyService.claim(idem)).thenReturn(true);

        courseService.saveCourseTable(List.of(idem), List.of(json));

        CourseSchedule student = courseJpaRepository
                .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear(
                        "student_009", mockYear, mockWeek)
                .orElseThrow(NoSuchElementException::new);

        assertThat(student)
                .extracting(
                        CourseSchedule::getTeacherId, CourseSchedule::getTeacherName,
                        CourseSchedule::getStudentId, CourseSchedule::getStudentName,
                        CourseSchedule::getMondayClassHour, CourseSchedule::getTuesdayClassHour, CourseSchedule::getWednesdayClassHour, CourseSchedule::getThursdayClassHour, CourseSchedule::getFridayClassHour,
                        CourseSchedule::getCourseYear, CourseSchedule::getWeekOfYear
                )
                .containsExactly(
                        "teacher_001", "Mr. Kim",
                        "student_009", "Irene Seo",
                        1, 2, 0, 0, 0,
                        mockYear, mockWeek
                );
    }

    @Test
    @DisplayName("레빗 엠큐-학생 이름 변경")
    void changeStudentName() throws InterruptedException {

        ChangeStudentNameRequest changeStudentNameRequest = new ChangeStudentNameRequest();
        changeStudentNameRequest.setMemberId("student_001");
        changeStudentNameRequest.setNewName("Click Kim");

        rabbitTemplate.convertAndSend("student.exchange", "student.name.update", changeStudentNameRequest);

        Thread.sleep(1000);

        CourseSchedule student010 = courseJpaRepository
                .findCourseScheduleByStudentId("student_001")
                .orElseThrow(NoSuchElementException::new);

        assertThat(student010)
                .extracting("studentName", "studentId")
                .containsExactly("Click Kim", "student_001");
    }

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

        courseService.saveCourseTable(List.of(sameIdem), List.of(json));
        courseService.saveCourseTable(List.of(sameIdem), List.of(json));

        // 기대: 1건만 존재
        long count = courseJpaRepository
                .findAllByCourseYearAndWeekOfYear(mockYear, mockWeek)
                .stream().filter(s -> "teacher_001".equals(s.getTeacherId())).count();

        assertThat(count).isEqualTo(1);
    }
}