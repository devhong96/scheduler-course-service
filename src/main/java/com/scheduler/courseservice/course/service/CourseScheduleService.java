package com.scheduler.courseservice.course.service;

import com.scheduler.courseservice.course.component.DateProvider;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;

@Service
@RequiredArgsConstructor
public class CourseScheduleService {

    private final ScheduleConflictValidator validator;
    private final RedissonClient redissonClient;
    private final CourseJpaRepository courseJpaRepository;
    private final DateProvider dateProvider;

    @Transactional
    public void updateSchedule(CourseRequestMessage courseMessage) {
        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        String teacherCacheKey = String.format("courseSchedules:%d:%d:teacher:%s",
                currentYear,
                currentWeek,
                courseMessage.getTeacherId());

        RBucket<List<CourseSchedule>> teacherBucket = redissonClient.getBucket(teacherCacheKey);

        List<CourseSchedule> scheduleList = Optional.ofNullable(teacherBucket.get())
                .orElseGet(() -> {
                    List<CourseSchedule> dataFromDb = courseJpaRepository
                            .findAllCourseScheduleByTeacherIdAndCourseYearAndWeekOfYear(
                                    courseMessage.getTeacherId(),
                                    currentYear, currentWeek);
                    // "기존 데이터"를 캐시에 채워넣기
                    teacherBucket.set(dataFromDb);
                    return dataFromDb;
                });

        // 해당 시간대에 다른 학생의 수업이 있는지 확인
        validator.validate(courseMessage, scheduleList);

        // 기존 데이터 조회 (중복 방지)
        Optional<CourseSchedule> existingSchedule = scheduleList.stream()
                .filter(s -> s.getStudentId().equals(courseMessage.getStudentId()))
                .findFirst();

        CourseSchedule courseSchedule;

        if (existingSchedule.isPresent()) {
            // 기존 데이터 업데이트
            courseSchedule = existingSchedule.get();
            courseSchedule.updateSchedule(courseMessage);
        } else {
            // 새로운 데이터 추가
            courseSchedule = CourseSchedule.create(courseMessage);
        }

        courseJpaRepository.save(courseSchedule);
        teacherBucket.delete();

    }
}
