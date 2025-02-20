package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String MEMBER_SERVICE = "memberService";

    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final DateProvider dateProvider;

    private final MemberServiceClient memberServiceClient;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    @CircuitBreaker(name = MEMBER_SERVICE, fallbackMethod = "fallback")
    public Page<StudentCourseResponse> findAllStudentsCourses(
            Pageable pageable
    ) {
        return courseRepository.findAllStudentsCourses(pageable);
    }

    private Page<StudentCourseResponse> fallback(Pageable pageable, Throwable t) {
        log.warn("Fallback triggered due to: {}", t.getMessage());

        return Page.empty(pageable);
    }

    @Override
    @Transactional
    public CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear) {

        TeacherInfo teacherInfo = memberServiceClient
                .findTeachersClasses(token);

        String teacherId = teacherInfo.getTeacherId();
        int finalYear = (year != null) ? year : dateProvider.getCurrentYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : dateProvider.getCurrentWeek();

        List<StudentCourseResponse> studentClassByTeacherName = courseRepository
                .getStudentClassByTeacherId(teacherId, finalYear, finalWeekOfYear);

        CourseList classList = CourseList.getInstance();

        for (StudentCourseResponse studentCourseResponse : studentClassByTeacherName) {
            classList.getMondayClassList().add(studentCourseResponse.getMondayClass());
            classList.getTuesdayClassList().add(studentCourseResponse.getTuesdayClass());
            classList.getWednesdayClassList().add(studentCourseResponse.getWednesdayClass());
            classList.getThursdayClassList().add(studentCourseResponse.getThursdayClass());
            classList.getFridayClassList().add(studentCourseResponse.getFridayClass());
        }

        return classList;
    }

    @Override
    public StudentCourseResponse findStudentClasses(
            String token
    ) {
        StudentInfo studentInfo = memberServiceClient
                .findStudentInfoByToken(token);

        String studentId = studentInfo.getStudentId();
        return courseRepository.getWeeklyCoursesByStudentId(studentId);
    }

    @Override
    @Transactional
    public void saveClassTable(
            String token, RegisterCourseRequest registerCourseRequest
    ) {
        duplicateClassValidator(registerCourseRequest);

        StudentInfo studentInfo = memberServiceClient
                .findStudentInfoByToken(token);

        CourseSchedule courseSchedule = CourseSchedule
                .create(registerCourseRequest, studentInfo.getTeacherId());

        courseJpaRepository.save(courseSchedule);
    }

    private void duplicateClassValidator(RegisterCourseRequest registerCourseRequest) {

        List<StudentCourseResponse> StudentCourseList = courseRepository.getAllStudentsWeeklyCoursesForComparison();

        for (StudentCourseResponse StudentCourseResponse : StudentCourseList) {
            if (isOverlapping(StudentCourseResponse, registerCourseRequest)) {
                throw new IllegalStateException("수업이 중복됩니다.");
            }
        }
    }

    private boolean isOverlapping(StudentCourseResponse existing, RegisterCourseRequest newClass) {
        return Objects.equals(existing.getMondayClass(), newClass.getMondayClass()) ||
                Objects.equals(existing.getTuesdayClass(), newClass.getTuesdayClass()) ||
                Objects.equals(existing.getWednesdayClass(), newClass.getWednesdayClass()) ||
                Objects.equals(existing.getThursdayClass(), newClass.getThursdayClass()) ||
                Objects.equals(existing.getFridayClass(), newClass.getFridayClass());
    }

}