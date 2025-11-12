package com.scheduler.courseservice.course.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.FRIDAY;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@IntegrationTest
class CourseQueryServiceTest {

    @Autowired
    private CourseQueryService courseQueryService;

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
    @DisplayName("학생의 클래스 찾기")
    void findStudentClasses() {

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");

        when(memberServiceClient.findStudentInfoByToken(token))
                .thenReturn(studentInfo);

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

    @Test
    @DisplayName("교사에게 할당된 수업 수")
    void findTeachersClasses() {

        // Given
        TeacherInfo teacherInfo = new TeacherInfo("teacher_001");

        when(memberServiceClient.findTeacherInfoByToken(token))
                .thenReturn(teacherInfo);

        CourseList teachersClasses = courseQueryService
                .findTeachersClasses(token, mockYear, mockWeek);

        int size = teachersClasses.getClassList(FRIDAY).size();

        assertThat(size).isEqualTo(1);
    }

}