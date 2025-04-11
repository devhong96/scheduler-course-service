package com.scheduler.courseservice.course.service;

import java.util.List;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeMemberNameDto;

public interface CourseService {

      void applyCourse(String token, UpsertCourseRequest upsertCourseRequest);

      void changeStudentName(ChangeMemberNameDto changeMemberNameDto);

      void saveCourseTable(List<String> message);
}