package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.*;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseQueryServiceImpl implements CourseQueryService {

    private final LocalDate localDate = LocalDate.now();
    private final CourseRepository courseRepository;
    private final MemberServiceClient memberServiceClient;

    @Override
    @Transactional
    public Page<StudentCourseResponse> findAllStudentsCourses(
            Pageable pageable, String keyword
    ) {
        return courseRepository.findAllStudentsCourses(pageable, keyword);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "teacherService", fallbackMethod = "fallbackFindTeachersClasses")
    public CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear) {

        String teacherId = memberServiceClient.findTeachersClasses(token).getTeacherId();

        int finalYear = (year != null) ? year : localDate.getYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear());

        List<StudentCourseResponse> studentClassByTeacherName = courseRepository
                .getStudentClassByTeacherId(teacherId, finalYear, finalWeekOfYear);

        CourseList classList = new CourseList();

        studentClassByTeacherName.forEach(course -> {
            classList.addClass(MONDAY, course.getMondayClassHour());
            classList.addClass(TUESDAY, course.getTuesdayClassHour());
            classList.addClass(WEDNESDAY, course.getWednesdayClassHour());
            classList.addClass(THURSDAY, course.getThursdayClassHour());
            classList.addClass(FRIDAY, course.getFridayClassHour());
        });

        return classList;
    }

    protected CourseList fallbackFindTeachersClasses(String token, Integer year, Integer weekOfYear, Throwable e) {
        log.warn("Fallback activated for findTeachersClasses. Reason: {}", e.getMessage());
        return new CourseList();
    }

    @Override
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackFindStudentClasses")
    public StudentCourseResponse findStudentClasses(
            String token, Integer year, Integer weekOfYear
    ) {
        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        String studentId = studentInfo.getStudentId();
        int finalYear = (year != null) ? year : localDate.getYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear());

        return courseRepository.getWeeklyCoursesByStudentId(studentId, finalYear, finalWeekOfYear);
    }

    protected StudentCourseResponse fallbackFindStudentClasses(
            String token, Integer year, Integer weekOfYear, Throwable e
    ) {
        log.warn("Fallback activated for findStudentClasses. Reason: {}", e.getMessage());

        return new StudentCourseResponse();
    }
}
