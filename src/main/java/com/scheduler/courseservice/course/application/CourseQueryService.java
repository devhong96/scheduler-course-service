package com.scheduler.courseservice.course.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

public interface CourseQueryService {

    Page<StudentCourseResponse> findAllStudentsCourses(Pageable pageable, String keyword);

    CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear);

    StudentCourseResponse findStudentClasses(String token, Integer year, Integer weekOfYear);

}
