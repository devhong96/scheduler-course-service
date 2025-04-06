package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.service.CourseQueryService;
import com.scheduler.courseservice.course.service.CourseService;
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

    @Operation(
            summary = "학생 수업 조회",
            description = "조회자의 금주 수업 조회"
    )
    @GetMapping("class")
    public ResponseEntity<StudentCourseResponse> findClass(
            @RequestHeader(AUTHORIZATION) String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekOfYear
    ) {
        return new ResponseEntity<>(courseQueryService.findStudentClasses(token, year, weekOfYear), OK);
    }

    @Operation(
            summary = "수업 제출",
            description = "수정, 저장 포함. redis를 이용하여 동시성과 중복을 검사한 후, kafka를 이용하여 db에 저장함."
    )
    @PostMapping("class")
    public ResponseEntity<Void> applyCourse(
            @RequestHeader(AUTHORIZATION) String token,
            @Valid @RequestBody UpsertCourseRequest upsertCourseRequest
    ) {
        courseService.applyCourse(token, upsertCourseRequest);
        return new ResponseEntity<>(OK);
    }
}
