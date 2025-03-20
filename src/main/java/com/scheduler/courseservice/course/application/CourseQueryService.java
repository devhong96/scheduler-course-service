package com.scheduler.courseservice.course.application;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.*;

public interface CourseQueryService {

    PageCourseResponse findAllStudentsCourses(Integer page, Integer size, String keyword);

    CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear);

    StudentCourseResponse findStudentClasses(String token, Integer year, Integer weekOfYear);

}
