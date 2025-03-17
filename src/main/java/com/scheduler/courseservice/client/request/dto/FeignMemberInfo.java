package com.scheduler.courseservice.client.request.dto;

import com.scheduler.courseservice.client.RoleType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class FeignMemberInfo {

    @Getter
    @Setter
    @ToString
    public static class StudentInfo {

        private String teacherId;
        private String teacherName;
        private String studentId;
        private String studentName;

        public StudentInfo(String teacherId, String teacherName, String studentId, String studentName) {
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.studentId = studentId;
            this.studentName = studentName;
        }
    }

    @Getter
    @Setter
    public static class TeacherInfo {

        private String teacherId;

        public TeacherInfo(String teacherId) {
            this.teacherId = teacherId;
        }
    }

    @Getter
    @Setter
    public static class MemberInfo {

        private RoleType roleType;
        private String memberId;

    }
}
