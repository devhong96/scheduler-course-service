package com.scheduler.courseservice.course.repository;

import com.scheduler.courseservice.course.domain.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseJpaRepository extends JpaRepository<CourseSchedule, Long> {

    Boolean existsByStudentIdAndWeekOfYearAndYear(String studentId, int currentWeek, int currentYear);
}