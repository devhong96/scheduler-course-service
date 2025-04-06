package com.scheduler.courseservice.course.service;

import java.util.List;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;

public interface CourseService {

      void applyCourse(String token, UpsertCourseRequest upsertCourseRequest);

      void changeStudentName(ChangeStudentName changeStudentName);

      void saveCourseTable(List<String> message);
}