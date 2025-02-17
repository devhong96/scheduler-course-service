package com.scheduler.courseservice.client.dto;

import com.scheduler.courseservice.client.RoleType;
import lombok.Getter;
import lombok.Setter;

public class FeignMemberInfo {

    @Getter
    @Setter
    public static class StudentInfo {

        private String studentId;
        private String studentName;
        private String teacherId;
        private String teacherName;
    }

    @Getter
    @Setter
    public static class TeacherInfo {

        private String teacherId;
    }

    @Getter
    @Setter
    public static class MemberInfo {

        private RoleType roleType;
        private String memberId;

    }
}
