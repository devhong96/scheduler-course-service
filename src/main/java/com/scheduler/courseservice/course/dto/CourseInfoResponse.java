package com.scheduler.courseservice.course.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

public class CourseInfoResponse {

    @Getter
    @Setter
    @ToString
    public static class StudentCourseResponse {

        private String studentId = "";
        private String studentName = "";
        private String teacherId = "";
        private String teacherName = "";

        private Integer mondayClassHour = 0;
        private Integer tuesdayClassHour = 0;
        private Integer wednesdayClassHour = 0;
        private Integer thursdayClassHour = 0;
        private Integer fridayClassHour = 0;

        private Integer courseYear = 0;
        private Integer weekOfYear = 0;

        public StudentCourseResponse() {
        }

        public StudentCourseResponse(String studentId, Integer mondayClassHour, Integer tuesdayClassHour,
                                     Integer wednesdayClassHour, Integer thursdayClassHour,
                                     Integer fridayClassHour) {
            this.studentId = studentId;
            this.mondayClassHour = mondayClassHour;
            this.tuesdayClassHour = tuesdayClassHour;
            this.wednesdayClassHour = wednesdayClassHour;
            this.thursdayClassHour = thursdayClassHour;
            this.fridayClassHour = fridayClassHour;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            StudentCourseResponse that = (StudentCourseResponse) o;
            return Objects.equals(studentId, that.studentId) && Objects.equals(studentName, that.studentName) && Objects.equals(teacherId, that.teacherId) && Objects.equals(teacherName, that.teacherName) && Objects.equals(mondayClassHour, that.mondayClassHour) && Objects.equals(tuesdayClassHour, that.tuesdayClassHour) && Objects.equals(wednesdayClassHour, that.wednesdayClassHour) && Objects.equals(thursdayClassHour, that.thursdayClassHour) && Objects.equals(fridayClassHour, that.fridayClassHour) && Objects.equals(courseYear, that.courseYear) && Objects.equals(weekOfYear, that.weekOfYear);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, studentName, teacherId, teacherName, mondayClassHour, tuesdayClassHour, wednesdayClassHour, thursdayClassHour, fridayClassHour, courseYear, weekOfYear);
        }
    }

    @Getter
    @Setter
    public static class CourseList {
        public enum Day {
            MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
        }

        private Map<Day, List<Integer>> classSchedule = new LinkedHashMap<>(); // 순서 보장

        public CourseList() {
            for (Day day : Day.values()) {
                classSchedule.put(day, new ArrayList<>());
            }
        }

        public void addClass(Day day, Integer classHour) {
            if (classHour != null) {
                classSchedule.get(day).add(classHour);
            }
        }

        public List<Integer> getClassList(Day day) {
            return new ArrayList<>(classSchedule.get(day));
        }
    }
}
