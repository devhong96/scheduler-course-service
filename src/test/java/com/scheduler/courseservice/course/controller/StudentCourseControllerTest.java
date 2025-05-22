package com.scheduler.courseservice.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.testSet.IntegrationTest;
import com.scheduler.courseservice.testSet.JwtTokenDto;
import com.scheduler.courseservice.testSet.TestJwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class StudentCourseControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtils testJwtUtils;

    @MockitoBean
    private MemberServiceClient memberServiceClient;

    @Test
    @WithMockUser(username = "student_001", password = "student001", roles = "STUDENT")
    @DisplayName("컨트롤러 : 학생 수업 조회")
    void findStudentClasses() throws Exception {

        String accessToken = getAccessToken();

        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");

        when(memberServiceClient.findStudentInfoByToken(accessToken))
                .thenReturn(studentInfo);

        mockMvc.perform(get("/student/class")
                        .header(AUTHORIZATION, accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student_001", password = "student001", roles = "STUDENT")
    @DisplayName("컨트롤러 : 학생 수업 조회")
    void applyCourse() throws Exception {

        String accessToken = getAccessToken();
        StudentInfo studentInfo = new StudentInfo("teacher_001", "Mr. Kim", "student_009", "Irene Seo");

        when(memberServiceClient.findStudentInfoByToken(accessToken))
                .thenReturn(studentInfo);

        UpsertCourseRequest request = new UpsertCourseRequest();
        request.setMondayClassHour(1);
        request.setTuesdayClassHour(4);
        request.setWednesdayClassHour(3);
        request.setThursdayClassHour(2);
        request.setFridayClassHour(5);

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/student/class")
                        .header(AUTHORIZATION, getAccessToken())
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private String getAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtTokenDto jwtTokenDto = testJwtUtils.generateToken(authentication);
        return "Bearer " + jwtTokenDto.getAccessToken();
    }

}