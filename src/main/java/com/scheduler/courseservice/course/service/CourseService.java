package com.scheduler.courseservice.course.service;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeMemberNameDto;

public interface CourseService {

      void applyCourse(String token, UpsertCourseRequest upsertCourseRequest);

      void changeStudentName(ChangeMemberNameDto changeMemberNameDto);

      void saveCourseTable(
              @Header(name = "Idempotency-Key", required = false) List<String> idemKeys,
              @Payload List<String> messages,
              Acknowledgment ack
      );
}