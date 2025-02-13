package com.scheduler.courseservice.course.domain;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.TeacherInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.RegisterCourseRequest;
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

    private String studentId;

    private String teacherId;

    private Integer mondayClass;

    private Integer tuesdayClass;

    private Integer wednesdayClass;

    private Integer thursdayClass;

    private Integer fridayClass;

    private Integer weekNumber;

    private Integer year;

    @Version
    private Long version;

    public static CourseSchedule create(RegisterCourseRequest request, TeacherInfo teacherInfo) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.teacherId = teacherInfo.getTeacherId();
        courseSchedule.mondayClass = request.getMondayClass();
        courseSchedule.tuesdayClass = request.getTuesdayClass();
        courseSchedule.wednesdayClass = request.getWednesdayClass();
        courseSchedule.thursdayClass = request.getThursdayClass();
        courseSchedule.fridayClass = request.getFridayClass();
        return courseSchedule;
    }

    @PostConstruct
    public void init() {
        this.year = LocalDate.now().getYear();
    }

}