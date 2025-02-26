package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.testSet.IntegrationTest;
import com.scheduler.courseservice.testSet.messaging.ChangeStudentNameRequest;
import com.scheduler.courseservice.testSet.messaging.TestRabbitConsumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.NoSuchElementException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@IntegrationTest
@Import(TestRabbitConsumer.class)
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRabbitConsumer testRabbitConsumer;

    @Spy
    private static WireMockServer wireMockServer;

    final static String token = "Bearer test-token";
    final static int mockYear = 2025;
    final static int mockWeek = 10;
    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @BeforeAll
    static void startWireMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8080));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("교사에게 할당된 수업 수")
    void findTeachersClasses() throws JsonProcessingException {

        // Given
        final String expectedResponse = objectMapper
                .writeValueAsString(new TeacherInfo("teacher_001"));

        stubFor(get(urlEqualTo("/feign-member/teacher/info"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
        ));

        CourseList teachersClasses = courseService
                .findTeachersClasses(token, mockYear, mockWeek);

        int size = teachersClasses.getFridayClassList().size();
        assertThat(2).isEqualTo(size);
    }

    @Test
    @DisplayName("학생의 클래스 찾기")
    void findStudentClasses() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "student_010", "Jack Kang"));

        stubFor(get(urlEqualTo("/feign-member/student/info"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                ));

        StudentCourseResponse studentClasses = courseService.findStudentClasses(token, mockYear, mockWeek);

        assertThat(studentClasses)
                .extracting(
                "studentId", "studentName",
                        "mondayClassHour", "tuesdayClassHour", "wednesdayClassHour", "thursdayClassHour", "fridayClassHour",
                        "courseYear", "weekOfYear")
                .containsExactly(
                        "student_010", "Jack Kang",
                        2, 3, 2, 1, 3,
                        2025, 10
                );

    }

    @Test
    @DisplayName("수업 저장")
    void saveClassTable() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "student_010", "Jack Kang"));

        stubFor(get(urlEqualTo("/feign-member/student/info"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                ));

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(4);
        upsertCourseRequest.setWednesdayClassHour(3);
        upsertCourseRequest.setThursdayClassHour(2);
        upsertCourseRequest.setFridayClassHour(5);

        courseService.saveClassTable(token, upsertCourseRequest);

        CourseSchedule student = courseJpaRepository
                .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear("student_010",
                        LocalDate.now().getYear(),
                        LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear()))
                .orElseThrow(NoSuchElementException::new);

        assertThat(student)
                .extracting(
                        CourseSchedule::getStudentId, CourseSchedule::getTeacherId, CourseSchedule::getStudentName,
                        CourseSchedule::getMondayClassHour, CourseSchedule::getTuesdayClassHour, CourseSchedule::getWednesdayClassHour, CourseSchedule::getThursdayClassHour, CourseSchedule::getFridayClassHour,
                        CourseSchedule::getCourseYear, CourseSchedule::getWeekOfYear
                )
                .containsExactly(
                        "student_010", "teacher_001", "Jack Kang",
                        1, 4, 3, 2, 5,
                        LocalDate.now().getYear(), LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear())
                );
    }

    @Test
    @DisplayName("수업 수정")
    void modifyClassTable() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "student_010", "Jack Kang"));

        stubFor(get(urlEqualTo("/feign-member/student/info"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                ));

        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(0);
        upsertCourseRequest.setTuesdayClassHour(0);
        upsertCourseRequest.setWednesdayClassHour(0);
        upsertCourseRequest.setThursdayClassHour(0);
        upsertCourseRequest.setFridayClassHour(0);

        courseService.modifyClassTable(token, upsertCourseRequest);

        CourseSchedule student = courseJpaRepository
                .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear(
                        "student_010",
                        2025,
                        10
                )
                .orElseThrow(NoSuchElementException::new);

        assertThat(student)
                .extracting(
                        CourseSchedule::getStudentId, CourseSchedule::getTeacherId, CourseSchedule::getStudentName,
                        CourseSchedule::getMondayClassHour, CourseSchedule::getTuesdayClassHour, CourseSchedule::getWednesdayClassHour, CourseSchedule::getThursdayClassHour, CourseSchedule::getFridayClassHour,
                        CourseSchedule::getCourseYear, CourseSchedule::getWeekOfYear
                )
                .containsExactly(
                        "student_010", "teacher_001", "Jack Kang",
                        0, 0, 0, 0, 0,
                        2025, 10
                );
    }

    @Test
    @DisplayName("레빗 엠큐-학생 이름 변경")
    void changeStudentName() throws InterruptedException {
        ChangeStudentNameRequest changeStudentNameRequest = new ChangeStudentNameRequest();
        changeStudentNameRequest.setStudentId("student_010");
        changeStudentNameRequest.setStudentName("Jack Kang");

        rabbitTemplate.convertAndSend("student.exchange", "student.name.update", changeStudentNameRequest);

        ChangeStudentNameRequest received = testRabbitConsumer.getReceivedMessage();

        Assertions.assertThat(received)
                .extracting("studentName", "studentId")
                .containsExactly("Jack Kang", "student_010");
    }
}