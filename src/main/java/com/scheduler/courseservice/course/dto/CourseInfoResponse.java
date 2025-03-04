package com.scheduler.courseservice.course.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourseInfoResponse {

    @Getter
    @Setter
    public static class StudentCourseResponse {

        private String studentId;

        private String studentName;

        private String teacherId;

        private String teacherName;

        private Integer mondayClassHour;

        private Integer tuesdayClassHour;

        private Integer wednesdayClassHour;

        private Integer thursdayClassHour;

        private Integer fridayClassHour;

        private Integer courseYear;

        private Integer weekOfYear;
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
