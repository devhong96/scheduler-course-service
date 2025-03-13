package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseQueryService;
import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("student")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseService courseService;
    private final CourseQueryService courseQueryService;

    @Operation(description = "학생 본인 금주 수업 조회")
    @GetMapping("class")
    public ResponseEntity<StudentCourseResponse> findClass(
            @RequestHeader(AUTHORIZATION) String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekOfYear
    ) {
        return new ResponseEntity<>(courseQueryService.findStudentClasses(token, year, weekOfYear), OK);
    }

    @Operation(description = "수업 제출")
    @PostMapping("class")
    public ResponseEntity<Void> applyCourse(
            @RequestHeader(AUTHORIZATION) String token,
            @Valid @RequestBody UpsertCourseRequest upsertCourseRequest
    ) {
        courseService.applyCourse(token, upsertCourseRequest);
        return new ResponseEntity<>(OK);
    }
}
