package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
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

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String MEMBER_SERVICE = "memberService";

    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;

    private final MemberServiceClient memberServiceClient;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    @CircuitBreaker(name = MEMBER_SERVICE, fallbackMethod = "fallback")
    public Page<StudentCourseResponse> findAllStudentsCourses(
            String token, Pageable pageable
    ) {

        MemberInfo memberInfo = memberServiceClient.findMemberInfoByToken(token);
        String memberId = memberInfo.getMemberId();

        return courseRepository.findAllStudentsCourses(memberId, pageable);
    }

    private Page<StudentCourseResponse> fallback(String token, Pageable pageable, Throwable t) {
        log.warn("Fallback triggered due to: {}", t.getMessage());

        return Page.empty(pageable);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = MEMBER_SERVICE, fallbackMethod = "fallback")
    public CourseList findTeachersClasses(String token) {

        StudentInfo studentInfo = memberServiceClient
                .findCourseByStudentNameAndCode(token);

        String teacherId = studentInfo.getTeacherId();

        List<StudentCourseResponse> studentClassByTeacherName
                = courseRepository.getStudentClassByTeacherId(teacherId);

        CourseList classList = CourseList.getInstance();

        classList.setStudentName(studentInfo.getStudentName());

        for (StudentCourseResponse studentCourseResponse : studentClassByTeacherName) {
            classList.getMondayClassList().add(studentCourseResponse.getMondayClass());
            classList.getTuesdayClassList().add(studentCourseResponse.getTuesdayClass());
            classList.getWednesdayClassList().add(studentCourseResponse.getWednesdayClass());
            classList.getThursdayClassList().add(studentCourseResponse.getThursdayClass());
            classList.getFridayClassList().add(studentCourseResponse.getFridayClass());
        }

        return classList;
    }

    private CourseList fallback(String token, Throwable t) {
        log.warn("Fallback triggered due to: {}", t.getMessage());

        CourseList fallbackClassList = CourseList.getInstance();
        fallbackClassList.setStudentName("Unknown Student");

        return fallbackClassList;
    }

    @Override
    public StudentCourseResponse findStudentClasses(
            String token
    ) {
        StudentInfo feignMemberInfo = memberServiceClient
                .findCourseByStudentNameAndCode(token);

        String studentId = feignMemberInfo.getStudentId();
        return courseRepository.getWeeklyCoursesByStudentId(studentId);
    }

    @Override
    @Transactional
    public void saveClassTable(
            String token, RegisterCourseRequest registerCourseRequest
    ) {
        duplicateClassValidator(registerCourseRequest);

        StudentInfo studentInfo = memberServiceClient.findCourseByStudentNameAndCode(token);

        TeacherInfo teacherInfo = memberServiceClient.findTeacherByStudentId(token, studentInfo.getStudentId());

        CourseSchedule courseSchedule = CourseSchedule.create(registerCourseRequest, teacherInfo);

        courseJpaRepository.save(courseSchedule);
    }

    private void duplicateClassValidator(RegisterCourseRequest registerCourseRequest) {

        List<StudentCourseResponse> StudentCourseList = courseRepository.getWeeklyCoursesForAllStudents();

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