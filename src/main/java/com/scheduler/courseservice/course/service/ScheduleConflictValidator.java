package com.scheduler.courseservice.course.service;

import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;

@Component
public class ScheduleConflictValidator {

    public void validate(CourseRequestMessage newSchedule, List<CourseSchedule> existingSchedules) {
        for (CourseSchedule existing : existingSchedules) {
            if (Objects.equals(existing.getStudentId(), newSchedule.getStudentId())) continue;

            checkSlot(newSchedule.getMondayClassHour(), existing.getMondayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getTuesdayClassHour(), existing.getTuesdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getWednesdayClassHour(), existing.getWednesdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getThursdayClassHour(), existing.getThursdayClassHour(), newSchedule, existing);
            checkSlot(newSchedule.getFridayClassHour(), existing.getFridayClassHour(), newSchedule, existing);
        }
    }

    private void checkSlot(Integer newHour, Integer existingHour, CourseRequestMessage newSchedule, CourseSchedule existing) {
        boolean isNewSlotValid = (newHour != null && newHour != 0);

        if (isNewSlotValid && Objects.equals(newHour, existingHour)) {
            throw new DuplicateCourseException(
                    String.format("Schedule conflict detected for teacher %s with existing student %s on same time slot",
                            newSchedule.getTeacherName(), existing.getStudentName())
            );
        }
    }
}