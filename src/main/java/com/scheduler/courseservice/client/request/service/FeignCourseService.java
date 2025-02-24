package com.scheduler.courseservice.client.request.service;


import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseExistenceResponse;
import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseReassignmentResponse;

public interface FeignCourseService {

    CourseReassignmentResponse validateStudentCoursesAndReassign(String teacherId, String studentId);

    CourseExistenceResponse existWeeklyCoursesByTeacherId(String teacherId);
}
