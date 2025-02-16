package com.scheduler.courseservice.course.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
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

    private Integer mondayClassHour;

    private Integer tuesdayClassHour;

    private Integer wednesdayClassHour;

    private Integer thursdayClassHour;

    private Integer fridayClassHour;

    private Integer weekOfYear;

    private Integer year;

    @Version
    private Long version;

    public static CourseSchedule create(RegisterCourseRequest request, TeacherInfo teacherInfo) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.teacherId = teacherInfo.getTeacherId();
        courseSchedule.mondayClassHour = request.getMondayClass();
        courseSchedule.tuesdayClassHour = request.getTuesdayClass();
        courseSchedule.wednesdayClassHour = request.getWednesdayClass();
        courseSchedule.thursdayClassHour = request.getThursdayClass();
        courseSchedule.fridayClassHour = request.getFridayClass();
        return courseSchedule;
    }

    public static CourseSchedule createBaseSchedule(StudentInfo studentInfo, int currentWeek, int currentYear) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.year = currentYear;
        courseSchedule.weekOfYear = currentWeek;
        courseSchedule.studentId = studentInfo.getStudentId();
        courseSchedule.teacherId = studentInfo.getTeacherId();
        return courseSchedule;
    }
}