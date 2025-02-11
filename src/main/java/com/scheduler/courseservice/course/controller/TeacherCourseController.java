package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("teacher")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final CourseService courseService;

    @Operation(description = "선생님 버전 조회")
    @GetMapping("class")
    public ResponseEntity<Page<StudentCourseResponse>> managePage (
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return new ResponseEntity<>(courseService.findStudentClassList(token, of(page - 1, size)), OK);
    }
}
