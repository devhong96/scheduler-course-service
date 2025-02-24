package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final DateProvider dateProvider;

    private final MemberServiceClient memberServiceClient;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    public Page<StudentCourseResponse> findAllStudentsCourses(
            Pageable pageable
    ) {
        Map<String, Object> cachedData = courseRepository.findAllStudentsCoursesCache(pageable);

        List<StudentCourseResponse> contents = (List<StudentCourseResponse>) cachedData.get("content");
        Long totalCount = (Long) cachedData.get("totalCount");

        return new PageImpl<>(contents, pageable, totalCount);
    }


    @Override
    @Transactional
    public CourseList findTeachersClasses(String token, Integer year, Integer weekOfYear) {

        TeacherInfo teacherInfo = memberServiceClient.findTeachersClasses(token);

        String teacherId = teacherInfo.getTeacherId();
        int finalYear = (year != null) ? year : dateProvider.getCurrentYear();
        int finalWeekOfYear = (weekOfYear != null) ? weekOfYear : dateProvider.getCurrentWeek();

        List<StudentCourseResponse> studentClassByTeacherName = courseRepository
                .getStudentClassByTeacherId(teacherId, finalYear, finalWeekOfYear);

        CourseList classList = CourseList.getInstance();

        for (StudentCourseResponse studentCourseResponse : studentClassByTeacherName) {
            classList.getMondayClassList().add(studentCourseResponse.getMondayClassHour());
            classList.getTuesdayClassList().add(studentCourseResponse.getTuesdayClassHour());
            classList.getWednesdayClassList().add(studentCourseResponse.getWednesdayClassHour());
            classList.getThursdayClassList().add(studentCourseResponse.getThursdayClassHour());
            classList.getFridayClassList().add(studentCourseResponse.getFridayClassHour());
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
            String token, UpsertCourseRequest upsertCourseRequest
    ) {
        duplicateClassValidator(upsertCourseRequest, null);

        StudentInfo studentInfo = memberServiceClient
                .findStudentInfoByToken(token);

        CourseSchedule courseSchedule = CourseSchedule
                .create(upsertCourseRequest, studentInfo.getTeacherId(), studentInfo);

        courseJpaRepository.save(courseSchedule);
    }

    @Override
    @Transactional
    public void modifyClassTable(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient
                .findStudentInfoByToken(token);

        CourseSchedule existingCourse = courseJpaRepository
                .findCourseScheduleByStudentId((studentInfo.getStudentId()))
                .orElseThrow(() -> new IllegalStateException("기존 수업을 찾을 수 없습니다."));

        duplicateClassValidator(upsertCourseRequest, existingCourse);

        existingCourse.updateSchedule(upsertCourseRequest);
    }

    private void duplicateClassValidator(UpsertCourseRequest upsertCourseRequest, CourseSchedule existingCourse) {

        List<StudentCourseResponse> studentCourseList = courseRepository.getAllStudentsWeeklyCoursesForComparison();

        for (StudentCourseResponse studentCourseResponse : studentCourseList) {

            if (existingCourse != null &&
                    studentCourseResponse.getStudentId().equals(existingCourse.getStudentId())) {
                continue;
            }

            if (isOverlapping(studentCourseResponse, upsertCourseRequest)) {
                throw new IllegalStateException("수업이 중복됩니다.");
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