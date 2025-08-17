package com.scheduler.courseservice.outbox.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

    CREATED, DELETED
}
