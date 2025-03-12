package com.scheduler.courseservice.course.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

import static com.scheduler.courseservice.course.dto.CourseInfoRequest.CourseRequestMessage;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@DynamicUpdate
@NoArgsConstructor(access = PROTECTED)
public class CourseSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String teacherId;

    @Column(nullable = false)
    private String teacherName;

    @Column(nullable = false)
    private Integer mondayClassHour;

    @Column(nullable = false)
    private Integer tuesdayClassHour;

    @Column(nullable = false)
    private Integer wednesdayClassHour;

    @Column(nullable = false)
    private Integer thursdayClassHour;

    @Column(nullable = false)
    private Integer fridayClassHour;

    @Column(nullable = false)
    private Integer weekOfYear;

    @Column(nullable = false)
    private Integer courseYear;

    @Version
    private Long version = 0L;


    public static CourseSchedule create(CourseRequestMessage request) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.studentId = request.getStudentId();
        courseSchedule.studentName = request.getStudentName();
        courseSchedule.teacherId = request.getTeacherId();
        courseSchedule.teacherName = request.getTeacherName();
        courseSchedule.mondayClassHour = request.getMondayClassHour();
        courseSchedule.tuesdayClassHour = request.getTuesdayClassHour();
        courseSchedule.wednesdayClassHour = request.getWednesdayClassHour();
        courseSchedule.thursdayClassHour = request.getThursdayClassHour();
        courseSchedule.fridayClassHour = request.getFridayClassHour();

        LocalDate now = LocalDate.now();
        courseSchedule.courseYear = now.getYear();
        courseSchedule.weekOfYear = now.get(WeekFields.of(Locale.getDefault()).weekOfYear());
        return courseSchedule;
    }

    public void updateSchedule(UpsertCourseRequest upsertCourseRequest) {
        this.mondayClassHour = upsertCourseRequest.getMondayClassHour();
        this.tuesdayClassHour = upsertCourseRequest.getTuesdayClassHour();
        this.wednesdayClassHour = upsertCourseRequest.getWednesdayClassHour();
        this.thursdayClassHour = upsertCourseRequest.getThursdayClassHour();
        this.fridayClassHour = upsertCourseRequest.getFridayClassHour();
    }

    public void updateStudentName(String studentName) {
        this.studentName = studentName;
    }
}