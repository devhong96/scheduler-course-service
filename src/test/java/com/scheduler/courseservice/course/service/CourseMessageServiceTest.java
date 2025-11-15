package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.outbox.service.IdempotencyService;
import com.scheduler.courseservice.testSet.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.mockWeek;
import static com.scheduler.courseservice.testSet.messaging.testDataSet.mockYear;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@IntegrationTest
class CourseMessageServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @Autowired
    private CourseMessageService courseMessageService;

    @Test
    void processMessage() throws Exception {

        // Given
        StudentInfo studentInfo = new StudentInfo(
                "teacher_001", "Mr. Kim",
                "student_009", "Irene Seo"
        );
        UpsertCourseRequest upsertCourseRequest = new UpsertCourseRequest();
        upsertCourseRequest.setMondayClassHour(1);
        upsertCourseRequest.setTuesdayClassHour(2);

        CourseRequestMessage message = new CourseRequestMessage(studentInfo, upsertCourseRequest);
        String json = objectMapper.writeValueAsString(message);
        String idem = UUID.randomUUID().toString();

        when(idempotencyService.claim(idem)).thenReturn(true);

        //When
        courseMessageService.processMessage(idem, json);

        //Then
        CourseSchedule student = courseJpaRepository
                .findCourseScheduleByStudentIdAndCourseYearAndWeekOfYear("student_009", mockYear, mockWeek)
                .orElseThrow();

        assertThat(student)
                .extracting(
                        CourseSchedule::getTeacherId, CourseSchedule::getStudentName,
                        CourseSchedule::getMondayClassHour, CourseSchedule::getTuesdayClassHour
                )
                .containsExactly(
                        "teacher_001", "Irene Seo",
                        1, 2
                );
    }
}