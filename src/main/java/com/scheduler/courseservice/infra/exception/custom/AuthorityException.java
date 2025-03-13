package com.scheduler.courseservice.infra.exception.custom;

public class AuthorityException extends RuntimeException {

    // 기본 생성자
    public AuthorityException() {
        super("권한이 없습니다.");
    }

    // 메시지를 받는 생성자
    public AuthorityException(String message) {
        super(message);
    }

    // 메시지와 원인(cause)를 받는 생성자
    public AuthorityException(String message, Throwable cause) {
        super(message, cause);
    }

    // 원인(cause)만 받는 생성자
    public AuthorityException(Throwable cause) {
        super(cause);
    }
}