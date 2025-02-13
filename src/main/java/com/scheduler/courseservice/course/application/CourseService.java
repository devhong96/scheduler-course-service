package com.scheduler.courseservice.course.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.FindStudentCourseInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

public interface CourseService {

      Page<StudentCourseResponse> findAllStudentsCourses(String token, Pageable pageable);

      CourseList findTeachersClasses(FindStudentCourseInfo findStudentCourseInfo);

      StudentCourseResponse findStudentClasses(FindStudentCourseInfo findStudentCourseInfo);

      void saveClassTable(RegisterCourseRequest registerCourseRequest);

}
