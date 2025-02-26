package com.scheduler.courseservice.course.repository;

import com.scheduler.courseservice.course.domain.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseJpaRepository extends JpaRepository<CourseSchedule, Long> {

    Boolean existsByTeacherIdAndWeekOfYearAndCourseYear(String teacherId, int currentWeek, int currentYear);

    Optional<CourseSchedule> findCourseScheduleByStudentId(String studentId);

    Optional<CourseSchedule> findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear(String studentId, Integer courseYear, Integer weekOfYear);
}