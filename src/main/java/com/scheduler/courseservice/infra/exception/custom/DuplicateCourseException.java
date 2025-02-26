package com.scheduler.courseservice.infra.exception.custom;

public class DuplicateCourseException extends RuntimeException {
    public DuplicateCourseException(String message) {
        super(message);
    }
}
