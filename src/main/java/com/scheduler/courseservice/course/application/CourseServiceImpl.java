package com.scheduler.courseservice.course.application;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.FindStudentCourseInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.CourseList;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final MemberServiceClient memberServiceClient;
    private final CourseRepository courseRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    public Page<StudentCourseResponse> findStudentClassList(
            String token, Pageable pageable
    ) {
        MemberInfo memberInfo = memberServiceClient.findMemberInfoByToken(token);
        String memberId = memberInfo.getMemberId();

        return courseRepository.getStudentCourseList(memberId, pageable);
    }

    @Override
    @Transactional
    public CourseList findTeachersClasses(FindStudentCourseInfo findStudentCourseInfo) {

        String studentName = findStudentCourseInfo.getStudentName();
        String password = findStudentCourseInfo.getPassword();

        StudentInfo studentInfo = memberServiceClient
                .findCourseByStudentName(studentName, password);

        String teacherId = studentInfo.getTeacherId();

        List<StudentCourseResponse> studentClassByTeacherName
                = courseRepository.getStudentClassByTeacherId(teacherId);

        CourseList classList = CourseList.getInstance();

        classList.setStudentName(studentName);

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
            FindStudentCourseInfo findStudentCourseInfo
    ) {
        String studentName = findStudentCourseInfo.getStudentName();
        String password = findStudentCourseInfo.getPassword();

        StudentInfo feignMemberInfo = memberServiceClient
                .findCourseByStudentName(studentName, password);

        String studentId = feignMemberInfo.getStudentId();
        return courseRepository.getStudentClassByStudentId(studentId);
    }

    @Override
    @Transactional
    public void saveClassTable(RegisterCourseRequest registerCourseRequest) {

        duplicateClassValidator(registerCourseRequest);

        String studentName = registerCourseRequest.getStudentName();

        String password = registerCourseRequest.getPassword();

        StudentInfo studentInfo = memberServiceClient.findStudentByStudentNameAndPassword(studentName, password);

        TeacherInfo teacherInfo = memberServiceClient.findTeacherByStudentId(studentInfo.getStudentId());

        CourseSchedule courseSchedule = CourseSchedule.create(registerCourseRequest, teacherInfo);

        courseJpaRepository.save(courseSchedule);

    }

    private void duplicateClassValidator(RegisterCourseRequest registerCourseRequest) {

        List<StudentCourseResponse> allClassDTO = courseRepository.getStudentClass();

        for (StudentCourseResponse classDTO : allClassDTO) {
            if (isOverlapping(classDTO, registerCourseRequest)) {
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