package com.scheduler.courseservice.infra.exception.custom;

public class DuplicateCourseException extends RuntimeException {

    public DuplicateCourseException() {
        super("중복된 시간이 있습니다.");}

    public DuplicateCourseException(String message) {
        super(message);
    }
}
