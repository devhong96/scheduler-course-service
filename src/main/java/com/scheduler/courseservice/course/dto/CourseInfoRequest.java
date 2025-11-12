package com.scheduler.courseservice.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;

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

    @Getter
    @Setter
    public static class CourseRequestMessage {

        private String teacherId;

        private String teacherName;

        private String studentId;

        private String studentName;

        private Integer mondayClassHour;

        private Integer tuesdayClassHour;

        private Integer wednesdayClassHour;

        private Integer thursdayClassHour;

        private Integer fridayClassHour;

        public CourseRequestMessage() {
        }

        public CourseRequestMessage(StudentInfo studentInfo, UpsertCourseRequest upsertCourseRequest) {
            this.teacherId = studentInfo.getTeacherId();
            this.teacherName = studentInfo.getTeacherName();
            this.studentId = studentInfo.getStudentId();
            this.studentName = studentInfo.getStudentName();
            this.mondayClassHour = upsertCourseRequest.getMondayClassHour();
            this.tuesdayClassHour = upsertCourseRequest.getTuesdayClassHour();
            this.wednesdayClassHour = upsertCourseRequest.getWednesdayClassHour();
            this.thursdayClassHour = upsertCourseRequest.getThursdayClassHour();
            this.fridayClassHour = upsertCourseRequest.getFridayClassHour();
        }
    }
}
