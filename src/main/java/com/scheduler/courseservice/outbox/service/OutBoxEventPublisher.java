package com.scheduler.courseservice.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.courseservice.outbox.domain.EventType;
import com.scheduler.courseservice.outbox.domain.OutBox;
import com.scheduler.courseservice.outbox.domain.OutBoxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public void publish(EventType eventType, EventPayload eventPayload) {

        try {
            OutBox outBox = OutBox.create(
                    eventType,
                    objectMapper.writeValueAsString(eventPayload)
            );

            applicationEventPublisher.publishEvent(OutBoxEvent.of(outBox));

        } catch (Exception e) {
            log.error("[DataSerializer.serialize] = {}",  e.getMessage());
        }
    }
}
