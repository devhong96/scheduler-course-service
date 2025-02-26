package com.scheduler.courseservice.infra.exception;

import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class CourseException {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException e) {
        return new ResponseEntity<>(e.getMessage(), NOT_FOUND);
    }

    @ExceptionHandler(DuplicateCourseException.class)
    public ResponseEntity<String> handleDuplicateCourseException(DuplicateCourseException e) {
        return new ResponseEntity<>(e.getMessage(), FORBIDDEN);
    }
}
