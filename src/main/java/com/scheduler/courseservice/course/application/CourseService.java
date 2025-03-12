package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.course.dto.CourseInfoRequest;
import jakarta.validation.Valid;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;

public interface CourseService {

      void applyCourse(String token, UpsertCourseRequest upsertCourseRequest);

      void modifyCourse(String token, @Valid CourseInfoRequest.UpsertCourseRequest upsertCourseRequest);

      void changeStudentName(ChangeStudentName changeStudentName);
}