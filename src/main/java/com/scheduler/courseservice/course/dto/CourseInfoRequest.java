package com.scheduler.courseservice.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CourseInfoRequest {

    @Getter
    @Setter
    public static class UpsertCourseRequest {

        // 수업 시간
        @NotNull(message = "요일을 선택해 주세요")
        private Integer mondayClassHour;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer tuesdayClassHour;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer wednesdayClassHour;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer thursdayClassHour;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer fridayClassHour;
    }
}
