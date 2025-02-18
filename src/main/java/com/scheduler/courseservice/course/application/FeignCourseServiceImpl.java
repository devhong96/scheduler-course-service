package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Service
@RequiredArgsConstructor
public class FeignCourseServiceImpl implements FeignCourseService {

    private final DateProvider dateProvider;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    public Boolean validateStudentCoursesAndReassign(String teacherId, String studentId) {

        List<StudentCourseResponse> teacherCourses = courseRepository.getWeeklyCoursesForTeacher(teacherId);
        StudentCourseResponse studentCourses = courseRepository.getWeeklyCoursesByStudentId(studentId);

        if (teacherCourses == null || studentCourses == null) {
            throw new IllegalArgumentException("주간 수업 데이터를 찾을 수 없습니다.");
        }

        classValidator(teacherCourses, studentCourses);

        return true;
    }

    private void classValidator(
            List<StudentCourseResponse> weekCourseByTeacherId,
            StudentCourseResponse weeklyCoursesByStudentId
    ) {
        if (weeklyCoursesByStudentId == null) {
            throw new IllegalArgumentException("학생의 주간 수업 데이터가 존재하지 않습니다.");
        }

        for (StudentCourseResponse teacherCourse : weekCourseByTeacherId) {
            checkConflict("월요일", teacherCourse.getMondayClass(), weeklyCoursesByStudentId.getMondayClass());
            checkConflict("화요일", teacherCourse.getTuesdayClass(), weeklyCoursesByStudentId.getTuesdayClass());
            checkConflict("수요일", teacherCourse.getWednesdayClass(), weeklyCoursesByStudentId.getWednesdayClass());
            checkConflict("목요일", teacherCourse.getThursdayClass(), weeklyCoursesByStudentId.getThursdayClass());
            checkConflict("금요일", teacherCourse.getFridayClass(), weeklyCoursesByStudentId.getFridayClass());
        }
    }

    private void checkConflict(String day, Integer teacherClass, Integer studentClass) {
        if (teacherClass != null && teacherClass.equals(studentClass)) {
            throw new IllegalArgumentException("학생의 " + day + " 수업 중에 겹치는 날이 있습니다.");
        }
    }

    @Override
    public Boolean existWeeklyCoursesByTeacherId(String teacherId) {
        return courseJpaRepository.existsByTeacherIdAndWeekOfYearAndYear(
                teacherId,
                dateProvider.getCurrentWeek(),
                dateProvider.getCurrentYear()
        );
    }
}
