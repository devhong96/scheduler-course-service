package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("class")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(description = "학생 본인 금주 수업 조회")
    @GetMapping("findClass")
    public ResponseEntity<StudentCourseResponse> findClass(
            @RequestHeader("Authorization") String token
    ) {
        return new ResponseEntity<>(courseService.findStudentClasses(token), OK);
    }

    @Operation(description = "수업 제출")
    @PostMapping("save")
    public ResponseEntity<Void> submitForm(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody RegisterCourseRequest registerCourseRequest
    ) {
        courseService.saveClassTable(token, registerCourseRequest);
        return new ResponseEntity<>(OK);
    }
}
