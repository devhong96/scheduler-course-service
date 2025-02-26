package com.scheduler.courseservice.testSet.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangeStudentNameRequest {

    private String studentId;
    private String studentName;

}
