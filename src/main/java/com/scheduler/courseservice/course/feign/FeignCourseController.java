package com.scheduler.courseservice.course.feign;

import com.scheduler.courseservice.course.application.FeignCourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("feign-course")
@RequiredArgsConstructor
public class FeignCourseController {

    private final FeignCourseService feignCourseService;

    @Operation(summary = "선생님이 담당하는 학생들의 주간 수업 존재 여부 조회")
    @GetMapping("teacher/{teacherId}/courses")
    Boolean existWeeklyCoursesByTeacherId(
            @PathVariable String teacherId
    ) {
        return feignCourseService.existWeeklyCoursesByTeacherId(teacherId);
    }

    @Operation(summary = "학생과 선생님의 주간 코스 중복 확인 후, 변경")
    @PatchMapping("teacher/{teacherId}/student/{studentId}")
    Boolean validateStudentCoursesAndReassign(
            @PathVariable String teacherId,
            @PathVariable String studentId
    ) {
        return feignCourseService.validateStudentCoursesAndReassign(teacherId, studentId);
    }
}
