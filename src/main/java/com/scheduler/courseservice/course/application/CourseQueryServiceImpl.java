package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.*;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.data.domain.PageRequest.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseQueryServiceImpl implements CourseQueryService {

    private final DateProvider dateProvider;
    private final CourseRepository courseRepository;
    private final MemberServiceClient memberServiceClient;

    @Override
    @Transactional(readOnly = true)
    public Page<StudentCourseResponse> findAllStudentsCourses(
            Integer page, Integer size, String keyword
    ) {
        return courseRepository
                .findAllStudentsCourses(of(page - 1, size), keyword);

    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackFindStudentClasses")
    public StudentCourseResponse findStudentClasses(
            String token, Integer year, Integer weekOfYear
    ) {
        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        if (studentInfo == null) {
            throw new IllegalStateException("StudentInfo is null");
        }

        String studentId = studentInfo.getStudentId();

        int finalYear = (year != null) ? year : dateProvider.getCurrentYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : dateProvider.getCurrentWeek();

        return courseRepository.getWeeklyCoursesByStudentId(studentId, finalYear, finalWeekOfYear);
    }

    protected StudentCourseResponse fallbackFindStudentClasses(
            String token, Integer year, Integer weekOfYear, Throwable e
    ) {
        log.warn("Reason: ", e);

        return new StudentCourseResponse();
    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "teacherService", fallbackMethod = "fallbackFindTeachersClasses")
    public CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear) {

        String teacherId = memberServiceClient.findTeacherInfoByToken(token).getTeacherId();

        int finalYear = (year != null) ? year : dateProvider.getCurrentYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : dateProvider.getCurrentWeek();

        List<StudentCourseResponse> studentClassByTeacherName = courseRepository
                .getWeeklyCoursesByTeacherId(teacherId, finalYear, finalWeekOfYear);

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
}
