package com.scheduler.courseservice.course.dto;

import lombok.Getter;
import lombok.Setter;

public class FeignMemberRequest {

    @Getter
    @Setter
    public static class CourseReassignmentResponse {
        private Boolean exists;

        public CourseReassignmentResponse(Boolean exists) {
            this.exists = exists;
        }
    }

    @Getter
    @Setter
    public static class CourseExistenceResponse {
        private Boolean exists;

        public CourseExistenceResponse(Boolean exists) {
            this.exists = exists;
        }
    }
}
