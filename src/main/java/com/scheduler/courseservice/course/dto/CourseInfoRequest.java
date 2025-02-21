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
        private Integer mondayClass;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer tuesdayClass;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer wednesdayClass;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer thursdayClass;

        @NotNull(message = "요일을 선택해 주세요")
        private Integer fridayClass;
    }
}
