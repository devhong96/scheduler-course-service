package com.scheduler.courseservice.course.messaging;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

public class RabbitMQDto {

    @Getter
    @Setter
    public static class ChangeStudentName {

        private String studentId;

        @NotEmpty
        private String studentName;

        public ChangeStudentName(String studentId, String studentName) {
            this.studentId = studentId;
            this.studentName = studentName;
        }
    }
}
