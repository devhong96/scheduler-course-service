package com.scheduler.courseservice.client.controller;

import com.scheduler.courseservice.client.service.FeignCourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseExistenceResponse;
import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseReassignmentResponse;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("feign-member-course")
@RequiredArgsConstructor
public class FeignMemberController {

    private final FeignCourseService feignCourseService;

    @Operation(
            summary = "교사 수업 존재 여부 조회",
            description = "담당하는 학생들의 주간 수업 존재 여부 조회"
    )
    @GetMapping("teacher/{teacherId}/courses")
    public ResponseEntity<CourseExistenceResponse> existWeeklyCoursesByTeacherId(
            @PathVariable String teacherId
    ) {
        return new ResponseEntity<>(feignCourseService
                .existWeeklyCoursesByTeacherId(teacherId), OK);
    }

    @Operation(
            summary = "교사 변경",
            description = "학생과 선생님의 주간 코스 중복 확인 후, 변경"
    )
    @PatchMapping("teacher/{teacherId}/student/{studentId}")
    public ResponseEntity<CourseReassignmentResponse> validateStudentCoursesAndReassign(
            @PathVariable String teacherId,
            @PathVariable String studentId
    ) {
        return new ResponseEntity<>(feignCourseService
                .validateStudentCoursesAndReassign(teacherId, studentId), OK);
    }
}
