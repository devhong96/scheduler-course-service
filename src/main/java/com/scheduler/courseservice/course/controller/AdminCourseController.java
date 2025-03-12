package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseQueryService courseQueryService;

    @Operation(description = "관리자 버전 조회")
    @GetMapping("class")
    public ResponseEntity<Page<StudentCourseResponse>> managePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return new ResponseEntity<>(courseQueryService.findAllStudentsCourses(of(page - 1, size), keyword), OK);
    }
}
