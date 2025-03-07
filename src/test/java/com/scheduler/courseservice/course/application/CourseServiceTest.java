package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.testSet.IntegrationTest;
import com.scheduler.courseservice.testSet.messaging.ChangeStudentNameRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

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
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.FRIDAY;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
    private CourseJpaRepository courseJpaRepository;

    @Spy
    private static WireMockServer wireMockServer;

    final static String token = "Bearer test-token";
    final static int mockYear = 2025;
    final static int mockWeek = 9;

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

        int size = teachersClasses.getClassList(FRIDAY).size();
        assertThat(size).isEqualTo(1);
    }

    @Test
    @DisplayName("학생의 클래스 찾기")
    void findStudentClasses() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo"));

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
                        "student_009", "Irene Seo",
                        3, 2, 1, 4, 2,
                        2025, 9
                );

    }

    @Test
    @DisplayName("수업 저장")
    void saveClassTable() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo"));

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
                .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear("student_009",
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
                        "student_009", "teacher_001", "Irene Seo",
                        1, 4, 3, 2, 5,
                        LocalDate.now().getYear(), LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear())
                );
    }

    @Test
    @DisplayName("수업 수정")
    void modifyClassTable() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo"));

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
                        "student_009",
                        2025,
                        9
                )
                .orElseThrow(NoSuchElementException::new);

        assertThat(student)
                .extracting(
                        CourseSchedule::getStudentId, CourseSchedule::getTeacherId, CourseSchedule::getStudentName,
                        CourseSchedule::getMondayClassHour, CourseSchedule::getTuesdayClassHour, CourseSchedule::getWednesdayClassHour, CourseSchedule::getThursdayClassHour, CourseSchedule::getFridayClassHour,
                        CourseSchedule::getCourseYear, CourseSchedule::getWeekOfYear
                )
                .containsExactly(
                        "student_009", "teacher_001", "Irene Seo",
                        0, 0, 0, 0, 0,
                        2025, 9
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