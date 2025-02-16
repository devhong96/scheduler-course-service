package com.scheduler.courseservice.course.component;

import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;
import static com.scheduler.courseservice.course.domain.CourseSchedule.createBaseSchedule;

@Component
@RequiredArgsConstructor
public class CourseSchedulerService {

    private final MemberServiceClient memberServiceClient;
    private final CourseJpaRepository courseJpaRepository;
    private final DateProvider dateProvider;

    @Transactional
    @Scheduled(cron = "0 0 0 * * MON") // 매주 월요일 자정 실행
    public void addWeeklySchedulesForStudents() {
        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        List<StudentInfo> StudentInfoList = memberServiceClient.findAllStudentsForWeeklySchedule();

        List<CourseSchedule> schedules = new ArrayList<>();

        for (StudentInfo studentInfo : StudentInfoList) {
            if (!courseJpaRepository.existsByStudentIdAndWeekOfYearAndYear(studentInfo.getStudentId(), currentWeek, currentYear)) {
                schedules.add(createBaseSchedule(studentInfo, currentWeek, currentYear));
            }
        }
        courseJpaRepository.saveAll(schedules);
        schedules.clear();
    }
}
