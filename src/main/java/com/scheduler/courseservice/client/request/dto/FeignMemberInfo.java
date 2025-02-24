package com.scheduler.courseservice.client.request.dto;

import com.scheduler.courseservice.client.RoleType;
import lombok.Getter;
import lombok.Setter;

public class FeignMemberInfo {

    @Getter
    @Setter
    public static class StudentInfo {

        private String teacherId;

        private String studentId;
        private String studentName;
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
