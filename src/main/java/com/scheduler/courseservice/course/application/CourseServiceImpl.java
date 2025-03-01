package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
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
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList.Day.*;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final MemberServiceClient memberServiceClient;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    private final LocalDate localDate = LocalDate.now();

    @Override
    @Transactional
    public Page<StudentCourseResponse> findAllStudentsCourses(
            Pageable pageable
    ) {
        return courseRepository.findAllStudentsCourses(pageable);
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

    @Override
    @Transactional
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackSaveClassTable")
    public void saveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest
    ) {
        duplicateClassValidator(upsertCourseRequest, null);

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        CourseSchedule courseSchedule = CourseSchedule
                .create(upsertCourseRequest, studentInfo.getTeacherId(), studentInfo);

        courseJpaRepository.save(courseSchedule);
    }

    protected void fallbackSaveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Fallback activated for saveClassTable. Reason: {}", e.getMessage());

        throw new RuntimeException("수업 정보를 저장할 수 없습니다. 다시 시도해 주세요.");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackModifyClassTable")
    public void modifyClassTable(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);

        CourseSchedule existingCourse = courseJpaRepository
                .findCourseScheduleByStudentId((studentInfo.getStudentId()))
                .orElseThrow(() -> new IllegalStateException("기존 수업을 찾을 수 없습니다."));

        duplicateClassValidator(upsertCourseRequest, existingCourse);

        existingCourse.updateSchedule(upsertCourseRequest);
    }

    protected void fallbackModifyClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Fallback activated for modifyClassTable. Reason: {}", e.getMessage());

        throw new RuntimeException("수업 정보를 수정할 수 없습니다. 다시 시도해 주세요.");
    }

    @Override
    @Transactional
    public void changeStudentName(ChangeStudentName changeStudentName) {

        String studentId = changeStudentName.getStudentId();

        CourseSchedule courseSchedule = courseJpaRepository
                .findCourseScheduleByStudentId(studentId)
                .orElseThrow(NoSuchElementException::new);

        courseSchedule.updateStudentName(changeStudentName.getStudentName());
    }

    private void duplicateClassValidator(UpsertCourseRequest upsertCourseRequest, CourseSchedule existingCourse) {

        List<StudentCourseResponse> studentCourseList = courseRepository
                .getAllStudentsWeeklyCoursesForComparison(
                        localDate.getYear(), localDate.get(WeekFields.of(Locale.getDefault()).weekOfYear()));

        for (StudentCourseResponse studentCourseResponse : studentCourseList) {

            if (existingCourse != null &&
                    studentCourseResponse.getStudentId().equals(existingCourse.getStudentId())) {
                continue;
            }

            if (isOverlapping(studentCourseResponse, upsertCourseRequest)) {
                throw new DuplicateCourseException("수업이 중복됩니다.");
            }
        }
    }

    private boolean isOverlapping(StudentCourseResponse existing, UpsertCourseRequest newClass) {
        return Objects.equals(existing.getMondayClassHour(), newClass.getMondayClassHour()) ||
                Objects.equals(existing.getTuesdayClassHour(), newClass.getTuesdayClassHour()) ||
                Objects.equals(existing.getWednesdayClassHour(), newClass.getWednesdayClassHour()) ||
                Objects.equals(existing.getThursdayClassHour(), newClass.getThursdayClassHour()) ||
                Objects.equals(existing.getFridayClassHour(), newClass.getFridayClassHour());
    }

}