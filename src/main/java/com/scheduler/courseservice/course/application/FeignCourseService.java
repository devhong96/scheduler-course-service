package com.scheduler.courseservice.course.application;


public interface FeignCourseService {

    Boolean validateStudentCoursesAndReassign(String teacherId, String studentId);

    Boolean existWeeklyCoursesByTeacherId(String teacherId);
}
