package com.scheduler.courseservice.course.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@IntegrationTest
class CourseServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private DateProvider dateProvider;

    @Autowired
    private CourseService courseService;

    @MockitoBean
    private WireMockServer wireMockServer;

    final static String token = "Bearer test-token";
    final static int mockYear = 2024;
    final static int mockWeek = 10;

    @BeforeEach
    void setUp() {

        wireMockServer = new WireMockServer(wireMockConfig().port(8080));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @Test
    void findTeachersClasses() throws JsonProcessingException {

        TeacherInfo mockTeacherInfo = new TeacherInfo("teacherId");
        // Given
        final String expectedResponse = objectMapper.writeValueAsString(mockTeacherInfo);

        stubFor(get(urlEqualTo("/feign-member/teacher/class"))
                .withHeader("Authorization", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedResponse)
        ));

        CourseList teachersClasses = courseService
                .findTeachersClasses(token, mockYear, mockWeek);

        System.out.println("teachersClasses = " + teachersClasses.getFridayClassList());

    }

    @Test
    void findAllStudentsCourses() {
    }

    @Test
    void findStudentClasses() {
    }

    @Test
    void saveClassTable() {
    }

    @Test
    void modifyClassTable() {
    }

}