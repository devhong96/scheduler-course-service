package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.testSet.IntegrationTest;
import com.scheduler.courseservice.testSet.messaging.ChangeStudentNameRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

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
    void applyCourse() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");
        when(memberServiceClient.findStudentInfoByToken(token)).thenReturn(studentInfo);

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(4);
        upsertCourseRequest.setWednesdayClassHour(3);
        upsertCourseRequest.setThursdayClassHour(2);
        upsertCourseRequest.setFridayClassHour(5);

        courseService.applyCourse(token, upsertCourseRequest);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                "topic_course_schedule_logs",
                objectMapper.writeValueAsString(new CourseRequestMessage(
                        studentInfo,
                        upsertCourseRequest
                ))
        );

        SendResult<String, String> sendResult = future.get(5, SECONDS);

        assertThat(sendResult).isNotNull();
        assertThat(sendResult.getProducerRecord().value()).isNotNull();

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

        CourseRequestMessage courseRequestMessage = new CourseRequestMessage(studentInfo, upsertCourseRequest);

        String string = objectMapper.writeValueAsString(courseRequestMessage);

        List<String> objects = new ArrayList<>();

        objects.add(string);

        courseService.saveCourseTable(objects);


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
        changeStudentNameRequest.setStudentId("student_001");
        changeStudentNameRequest.setStudentName("Click Kim");

        rabbitTemplate.convertAndSend("student.exchange", "student.name.update", changeStudentNameRequest);

        Thread.sleep(1000);

        CourseSchedule student010 = courseJpaRepository
                .findCourseScheduleByStudentId("student_001")
                .orElseThrow(NoSuchElementException::new);

        Assertions.assertThat(student010)
                .extracting("studentName", "studentId")
                .containsExactly("Click Kim", "student_001");
    }

    @Test
    @DisplayName("race condition test")
    public void testConcurrentCourseApplicationRaceCondition() throws InterruptedException {

        int threadCount = 2;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        // 두 스레드 모두 동일한 시간대를 사용하도록 UpsertCourseRequest 생성
        UpsertCourseRequest request = new UpsertCourseRequest();
        // 모든 요일에 같은 클래스 시간으로 설정 (충돌이 일어나도록)
        request.setMondayClassHour(1);
        request.setTuesdayClassHour(1);
        request.setWednesdayClassHour(1);
        request.setThursdayClassHour(1);
        request.setFridayClassHour(1);

        // applyCourse 호출 시 Kafka로 전송되는 JSON 메시지를 수집할 리스트
        List<String> kafkaMessages = Collections.synchronizedList(new ArrayList<>());

        when(memberServiceClient.findStudentInfoByToken("token_student_1"))
                .thenReturn(new StudentInfo("teacher123", "teacherName", "student1", "student1"));

        Runnable task1 = () -> {
            try {
                startLatch.await();
                // token_student_1으로 수업 신청
                courseService.applyCourse("token_student_1", request);
                // 실제 Kafka 전송 대신, 메시지 생성 후 리스트에 추가
                StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken("token_student_1");

                CourseRequestMessage msg1 = new CourseRequestMessage(studentInfo, request);
                String json = objectMapper.writeValueAsString(msg1);
                kafkaMessages.add(json);
            } catch (Exception e) {
                throw new NoSuchElementException(e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        };

        when(memberServiceClient.findStudentInfoByToken("token_student_2"))
                .thenReturn(new StudentInfo("teacher123", "teacherName", "student2", "student2"));

        Runnable task2 = () -> {
            try {
                startLatch.await();
                courseService.applyCourse("token_student_2", request);

                StudentInfo student2Info = memberServiceClient.findStudentInfoByToken("token_student_2");
                CourseRequestMessage msg2 = new CourseRequestMessage(student2Info, request);
                String json = objectMapper.writeValueAsString(msg2);
                kafkaMessages.add(json);
            } catch (Exception e) {
                throw new NoSuchElementException(e.getMessage());
            } finally {
                finishLatch.countDown();
            }
        };

        // 두 스레드 시작 준비 및 실행
        executor.submit(task1);
        executor.submit(task2);
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Kafka 메시지 소비 시나리오를 모의하여, 저장 로직을 실행합니다.
        // 두 메시지 중 하나가 먼저 등록되어, 두 번째 메시지에서 충돌이 발생해야 합니다.
        try {
            courseService.saveCourseTable(kafkaMessages);
            fail("충돌로 인해 RuntimeException 이 발생해야 합니다.");
        } catch (RuntimeException e) {
            assertTrue("예외 메시지에 'Schedule conflict detected'가 포함되어야 합니다.",
                    e.getMessage().contains("Schedule conflict detected"));
        }
    }
}