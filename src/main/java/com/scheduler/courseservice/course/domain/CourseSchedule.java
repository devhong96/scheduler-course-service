package com.scheduler.courseservice.course.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

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
    private String teacherId;

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
    private Integer year;

    @Version
    private Long version;


    public static CourseSchedule create(
            UpsertCourseRequest request, String teacherId, String studentId
    ) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.teacherId = teacherId;
        courseSchedule.studentId = studentId;
        courseSchedule.mondayClassHour = request.getMondayClass();
        courseSchedule.tuesdayClassHour = request.getTuesdayClass();
        courseSchedule.wednesdayClassHour = request.getWednesdayClass();
        courseSchedule.thursdayClassHour = request.getThursdayClass();
        courseSchedule.fridayClassHour = request.getFridayClass();
        return courseSchedule;
    }

    public void updateSchedule(UpsertCourseRequest upsertCourseRequest) {
        this.mondayClassHour = upsertCourseRequest.getMondayClass();
        this.tuesdayClassHour = upsertCourseRequest.getTuesdayClass();
        this.wednesdayClassHour = upsertCourseRequest.getWednesdayClass();
        this.thursdayClassHour = upsertCourseRequest.getThursdayClass();
        this.fridayClassHour = upsertCourseRequest.getFridayClass();
    }
}