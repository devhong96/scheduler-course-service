package com.scheduler.courseservice.outbox.service;

import lombok.Getter;
import lombok.Setter;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;

@Getter
@Setter
public class CourseCreatedEventPayload implements EventPayload {

    private String teacherId;

    private String teacherName;

    private String studentId;

    private String studentName;

    private Integer mondayClassHour;

    private Integer tuesdayClassHour;

    private Integer wednesdayClassHour;

    private Integer thursdayClassHour;

    private Integer fridayClassHour;

    public CourseCreatedEventPayload(StudentInfo studentInfo, UpsertCourseRequest upsertCourseRequest) {
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
