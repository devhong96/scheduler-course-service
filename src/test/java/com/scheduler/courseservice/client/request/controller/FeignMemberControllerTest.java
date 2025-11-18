package com.scheduler.courseservice.client.request.controller;

import com.scheduler.courseservice.client.service.FeignCourseService;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseExistenceResponse;
import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseReassignmentResponse;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class FeignMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeignCourseService feignCourseService;

    @Test
    @WithMockUser
    @DisplayName("선생님이 담당하는 학생들의 주간 수업 존재 여부 조회")
    void existWeeklyCoursesByTeacherId() throws Exception {

        CourseExistenceResponse mockResponse = new CourseExistenceResponse(true);

        String teacherId = "test-teacher";
        given(feignCourseService.existWeeklyCoursesByTeacherId(teacherId))
                .willReturn(mockResponse);

        mockMvc.perform(
                        get("/feign-member-course/teacher/{teacherId}/courses", teacherId)
                                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("exists").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("학생과 선생님의 주간 코스 중복 확인")
    void validateStudentCoursesAndReassign() throws Exception {

        String teacherId = "test-teacher";
        String studentId = "test-student";

        CourseReassignmentResponse mockResponse = new CourseReassignmentResponse(true);

        given(feignCourseService.validateStudentCoursesAndReassign(teacherId, studentId))
                .willReturn(mockResponse);

        mockMvc.perform(

                        patch("/feign-member-course/teacher/{teacherId}/student/{studentId}", teacherId, studentId)
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("exists").value(true));
    }
}