package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.course.dto.CourseInfoRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;

public interface CourseService {

      Page<StudentCourseResponse> findAllStudentsCourses(Pageable pageable);

      CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear);

      StudentCourseResponse findStudentClasses(String token, Integer year, Integer weekOfYear);

      void saveClassTable(String token, UpsertCourseRequest upsertCourseRequest);

      void modifyClassTable(String token, @Valid CourseInfoRequest.UpsertCourseRequest upsertCourseRequest);

      void changeStudentName(ChangeStudentName changeStudentName);
}