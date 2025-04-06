package com.scheduler.courseservice.course.service;

import org.springframework.data.domain.Page;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

public interface CourseQueryService {

    Page<StudentCourseResponse> findAllStudentsCourses(Integer page, Integer size, String keyword);

    CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear);

    StudentCourseResponse findStudentClasses(String token, Integer year, Integer weekOfYear);

}
