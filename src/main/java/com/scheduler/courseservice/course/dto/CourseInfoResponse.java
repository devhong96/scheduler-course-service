package com.scheduler.courseservice.course.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class CourseInfoResponse {

    @Getter
    @Setter
    public static class StudentCourseResponse {

        private String studentId;

        private Integer mondayClassHour;

        private Integer tuesdayClassHour;

        private Integer wednesdayClassHour;

        private Integer thursdayClassHour;

        private Integer fridayClassHour;
    }

    @Getter
    @Setter
    public static class CourseList {

        private List<Integer> mondayClassList = new ArrayList<>();
        private List<Integer> tuesdayClassList = new ArrayList<>();
        private List<Integer> wednesdayClassList = new ArrayList<>();
        private List<Integer> thursdayClassList = new ArrayList<>();
        private List<Integer> fridayClassList = new ArrayList<>();

        public static CourseList getInstance(){
            return new CourseList();
        }
    }
}
