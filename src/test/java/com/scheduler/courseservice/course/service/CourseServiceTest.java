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
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private IdempotencyService idempotencyService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

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

        when(memberServiceClient.findStudentInfoByToken(TEST_TOKEN_1)).thenReturn(studentInfo);

        StudentInfo result = memberServiceClient.findStudentInfoByToken(TEST_TOKEN_1);

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
        when(memberServiceClient.findStudentInfoByToken(TEST_TOKEN_1)).thenReturn(studentInfo);

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(4);
        upsertCourseRequest.setWednesdayClassHour(3);
        upsertCourseRequest.setThursdayClassHour(2);
        upsertCourseRequest.setFridayClassHour(5);

        courseService.applyCourse(TEST_TOKEN_1, upsertCourseRequest);

        ArgumentCaptor<EventPayload> payloadCaptor = ArgumentCaptor.forClass(EventPayload.class);

        verify(outBoxEventPublisher, times(1)).publish(
                eq(EventType.CREATED),
                payloadCaptor.capture()
        );

        EventPayload capturedPayload = payloadCaptor.getValue();

        assertThat(capturedPayload).isInstanceOf(CourseCreatedEventPayload.class);

        // 3-2. 페이로드 내부의 데이터가 예상과 일치하는지 확인
        CourseCreatedEventPayload coursePayload = (CourseCreatedEventPayload) capturedPayload;
        assertThat(coursePayload.getTeacherId()).isEqualTo(studentInfo.getTeacherId());
        assertThat(coursePayload.getMondayClassHour()).isEqualTo(upsertCourseRequest.getMondayClassHour());

    }

    @Test
    @DisplayName("수업 수정")
    void saveCourseTable() throws JsonProcessingException {

        Acknowledgment mockAck = mock(Acknowledgment.class);

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

        courseService.saveCourseTable(List.of(idem), List.of(json), mockAck);

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
                        "teacher_001", "Mr.Kim",
                        "student_009", "Irene_Seo",
                        1, 2, 0, 0, 0,
                        mockYear, mockWeek
                );
    }

    @Test
    @DisplayName("레빗 엠큐-학생 이름 변경")
    void changeStudentName() throws InterruptedException {

        ChangeStudentNameRequest changeStudentNameRequest = new ChangeStudentNameRequest();
        changeStudentNameRequest.setMemberId("student_001");
        changeStudentNameRequest.setNewName("Click_Kim");

        rabbitTemplate.convertAndSend("student.exchange", "student.name.update", changeStudentNameRequest);

        Thread.sleep(1000);

        CourseSchedule student010 = courseJpaRepository
                .findCourseScheduleByStudentId("student_001")
                .orElseThrow(NoSuchElementException::new);

        assertThat(student010)
                .extracting("studentName", "studentId")
                .containsExactly("Click_Kim", "student_001");
    }

}