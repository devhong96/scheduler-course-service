package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("student")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseService courseService;

    @Operation(description = "학생 본인 금주 수업 조회")
    @GetMapping("class")
    public ResponseEntity<StudentCourseResponse> findClass(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekOfYear
    ) {
        return new ResponseEntity<>(courseService.findStudentClasses(token, year, weekOfYear), OK);
    }

    @Operation(description = "수업 제출")
    @PostMapping("class")
    public ResponseEntity<Void> submitCourse(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpsertCourseRequest upsertCourseRequest
    ) {
        courseService.saveClassTable(token, upsertCourseRequest);
        return new ResponseEntity<>(OK);
    }

    @Operation(description = "수업 변경")
    @PatchMapping("class")
    public ResponseEntity<Void> modifyCourse(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpsertCourseRequest upsertCourseRequest
    ) {
        courseService.modifyClassTable(token, upsertCourseRequest);
        return new ResponseEntity<>(OK);
    }
}
