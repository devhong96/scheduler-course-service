package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.*;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseService courseService;

    @Spy
    private static WireMockServer wireMockServer;

    final static String token = "Bearer test-token";
    final static int mockYear = 2025;
    final static int mockWeek = 10;

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

        assertThat(studentClasses.getStudentId()).isEqualTo("student_010");
        assertThat(studentClasses.getStudentName()).isEqualTo("Jack Kang");

    }

}