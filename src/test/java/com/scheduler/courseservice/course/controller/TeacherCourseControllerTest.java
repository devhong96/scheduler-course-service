package com.scheduler.courseservice.course.controller;

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
import static com.scheduler.courseservice.client.dto.FeignMemberInfo.TeacherInfo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TeacherCourseControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtils testJwtUtils;

    @MockitoBean
    private MemberServiceClient memberServiceClient;

    @Test
    @WithMockUser(username = "test_teacher", password = "teacher", roles = "TEACHER")
    @DisplayName("컨트롤러 : 교사 수업 조회")
    void findTeachersClasses() throws Exception {

        String accessToken = getAccessToken();

        TeacherInfo teacherInfo = new TeacherInfo("teacherId");

        when(memberServiceClient.findTeacherInfoByToken(accessToken))
                .thenReturn(teacherInfo);

        mockMvc.perform(get("/teacher/class")
                        .header(AUTHORIZATION, getAccessToken()))
                .andExpect(status().isOk());
    }

    private String getAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtTokenDto jwtTokenDto = testJwtUtils.generateToken(authentication);
        return "Bearer " + jwtTokenDto.getAccessToken();
    }
}