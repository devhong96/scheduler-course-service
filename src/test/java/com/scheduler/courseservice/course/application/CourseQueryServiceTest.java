package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.FRIDAY;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@IntegrationTest
class CourseQueryServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseQueryService courseQueryService;

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
    @DisplayName("학생의 클래스 찾기")
    void findStudentClasses() throws JsonProcessingException {

        final String expectedResponse = objectMapper
                .writeValueAsString(
                        new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo")
                );

        stubFor(get(urlEqualTo("/feign-member/student/info"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
                ));

        StudentCourseResponse studentClasses = courseQueryService.findStudentClasses(token, mockYear, mockWeek);

        assertThat(studentClasses)
                .extracting(
                        "studentId", "studentName",
                        "mondayClassHour", "tuesdayClassHour", "wednesdayClassHour", "thursdayClassHour", "fridayClassHour",
                        "courseYear", "weekOfYear")
                .containsExactly(
                        "student_009", "Irene Seo",
                        3, 2, 1, 4, 2,
                        mockYear, mockWeek
                );
    }


    void findAllStudentsCourses() {
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

        CourseList teachersClasses = courseQueryService
                .findTeachersClasses(token, mockYear, mockWeek);

        int size = teachersClasses.getClassList(FRIDAY).size();

        assertThat(size).isEqualTo(1);
    }

}