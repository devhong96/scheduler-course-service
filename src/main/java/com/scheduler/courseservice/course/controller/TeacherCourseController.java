package com.scheduler.courseservice.course.controller;

import com.scheduler.courseservice.course.application.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("teacher")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final CourseService courseService;

    @Operation(description = "선생님 버전 조회")
    @GetMapping("class")
    public ResponseEntity<CourseList> managePage (
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekOfYear
    ) {
        return new ResponseEntity<>(courseService.findTeachersClasses(token, year, weekOfYear), OK);
    }
}
