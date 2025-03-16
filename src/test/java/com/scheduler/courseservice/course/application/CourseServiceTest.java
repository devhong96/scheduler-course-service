package com.scheduler.courseservice.course.application;

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
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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

    @Mock
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


//    @Test
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

//    @Test
    @DisplayName("수업 전달")
    void applyCourse() throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");
        final String expectedResponse = objectMapper
                .writeValueAsString(studentInfo);

        stubFor(post(urlEqualTo("/feign-member/student/info"))
                .withHeader(AUTHORIZATION, matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                ));

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
        upsertCourseRequest.setMondayClassHour(0);
        upsertCourseRequest.setTuesdayClassHour(0);
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
                        0, 0, 0, 0, 0,
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
}