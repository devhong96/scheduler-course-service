package com.scheduler.courseservice.course.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

public class RabbitMQDto {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeMemberNameDto {

        private String memberId;

        private String oldName;

        private String newName;

        private Boolean processed;

    }
}
