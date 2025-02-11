package com.scheduler.courseservice.client;

import lombok.Getter;

@Getter
public enum RoleType {

    ADMIN("관리자"),
    TEACHER("선생님");

    private final String description;

    RoleType(String description) {
        this.description = description;
    }
}
