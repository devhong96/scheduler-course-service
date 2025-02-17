package com.scheduler.courseservice.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class CourseInfoRequest {

    @Getter
    @Setter
    public static class CourseRequest {

        // 학생 이름
        private String studentName;

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

    @Getter
    @Setter
    public static class RegisterCourseRequest {

        // 학생 이름
        private String studentName;

        private String password;

        @JsonIgnore
        private String teacherId;

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
