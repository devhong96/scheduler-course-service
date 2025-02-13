package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @Operation(description = "관리자 버전 조회")
    @GetMapping("class")
    public ResponseEntity<Page<StudentCourseResponse>> managePage(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = of(page - 1, size);
        return new ResponseEntity<>(courseService.findAllStudentsCourses(token, pageRequest), OK);
    }
}
