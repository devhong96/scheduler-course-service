package com.scheduler.courseservice.client.request.service;

import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseExistenceResponse;
import static com.scheduler.courseservice.course.dto.FeignMemberRequest.CourseReassignmentResponse;

@Service
@RequiredArgsConstructor
public class FeignCourseServiceImpl implements FeignCourseService {

    private final DateProvider dateProvider;
    private final RedissonClient redissonClient;

    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    public CourseReassignmentResponse validateStudentCoursesAndReassign(String teacherId, String studentId) {

        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        String cacheKey = "courseSchedules:" + currentYear + ":" + currentWeek;
        RBucket<List<CourseSchedule>> bucket = redissonClient.getBucket(cacheKey);

        List<CourseSchedule> redisScheduleList = bucket.get();
        if (redisScheduleList == null) {
            redisScheduleList = courseJpaRepository.findAllByCourseYearAndWeekOfYear(currentYear, currentWeek);
            bucket.set(redisScheduleList);
        }

        // 교사 주간 수업 조회
        List<StudentCourseResponse> teacherCourses = redisScheduleList.stream()
                .filter(schedule -> schedule.getTeacherId().equals(teacherId))
                .map(this::mapToStudentCourseResponse)
                .collect(Collectors.toList());

        // 학생 주간 수업 조회
        StudentCourseResponse studentCourses = redisScheduleList.stream()
                .filter(schedule -> schedule.getStudentId().equals(studentId))
                .findFirst()
                .map(this::mapToStudentCourseResponse)
                .orElse(null);

        if (teacherCourses.isEmpty() || studentCourses == null) {
            throw new IllegalArgumentException("주간 수업 데이터를 찾을 수 없습니다.");
        }

        classValidator(teacherCourses, studentCourses);

        return new CourseReassignmentResponse(true);
    }

    private StudentCourseResponse mapToStudentCourseResponse(CourseSchedule schedule) {
        return new StudentCourseResponse(
                schedule.getStudentId(),
                schedule.getFridayClassHour(),
                schedule.getMondayClassHour(),
                schedule.getTuesdayClassHour(),
                schedule.getWednesdayClassHour(),
                schedule.getThursdayClassHour()
        );
    }

    private void classValidator(
            List<StudentCourseResponse> weekCourseByTeacherId,
            StudentCourseResponse weeklyCoursesByStudentId
    ) {
        if (weeklyCoursesByStudentId == null) {
            throw new IllegalArgumentException("학생의 주간 수업 데이터가 존재하지 않습니다.");
        }

        for (StudentCourseResponse teacherCourse : weekCourseByTeacherId) {
            checkConflict("월요일", teacherCourse.getMondayClassHour(), weeklyCoursesByStudentId.getMondayClassHour());
            checkConflict("화요일", teacherCourse.getTuesdayClassHour(), weeklyCoursesByStudentId.getTuesdayClassHour());
            checkConflict("수요일", teacherCourse.getWednesdayClassHour(), weeklyCoursesByStudentId.getWednesdayClassHour());
            checkConflict("목요일", teacherCourse.getThursdayClassHour(), weeklyCoursesByStudentId.getThursdayClassHour());
            checkConflict("금요일", teacherCourse.getFridayClassHour(), weeklyCoursesByStudentId.getFridayClassHour());
        }
    }

    private void checkConflict(String day, Integer teacherClass, Integer studentClass) {
        if (teacherClass != null && teacherClass.equals(studentClass)) {
            throw new IllegalArgumentException("학생의 " + day + " 수업 중에 겹치는 날이 있습니다.");
        }
    }

    @Override
    public CourseExistenceResponse existWeeklyCoursesByTeacherId(String teacherId) {
        Boolean exists = courseJpaRepository.existsByTeacherIdAndWeekOfYearAndCourseYear(
                teacherId,
                dateProvider.getCurrentWeek(),
                dateProvider.getCurrentYear()
        );

        return new CourseExistenceResponse(exists);
    }
}
