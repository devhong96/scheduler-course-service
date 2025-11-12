package com.scheduler.courseservice.outbox.service;

import com.scheduler.courseservice.outbox.domain.OutBox;
import com.scheduler.courseservice.outbox.domain.OutBoxEvent;
import com.scheduler.courseservice.outbox.repository.OutBoxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    @Value("${spring.kafka.topics.course.apply}")
    private String courseApplyTopic;

    private final OutBoxJpaRepository outBoxJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutBoxEvent outBoxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent={}", outBoxEvent);
        outBoxJpaRepository.save(outBoxEvent.getOutBox());
    }

    @Async("messageRelayPublishEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutBoxEvent outBoxEvent) {
        publishEvent(outBoxEvent.getOutBox());
    }

    private void publishEvent(OutBox outbox) {
        try {

            Message<String> message = MessageBuilder.withPayload(outbox.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, courseApplyTopic)
                    .setHeader("Idempotency-Key", outbox.getIdempotency())
                    .setHeader("Event-Type", outbox.getEventType().name())
                    .build();

            kafkaTemplate.executeInTransaction(kt -> {
                kt.send(message);
                return null;
            });

            outBoxJpaRepository.delete(outbox);
        } catch (Exception e) {
            log.error("[MessageRelay.publishEvent] outbox={}", outbox, e);
        }
    }

    @Scheduled(
            fixedDelay = 10, initialDelay = 5,
            timeUnit = TimeUnit.SECONDS, scheduler = "messageRelayPublishPendingEventExecutor"
    )
    public void publishPendingEvent() {

        List<OutBox> outboxes = outBoxJpaRepository.findAll();

        for (OutBox outbox : outboxes) {
            publishEvent(outbox);
        }
    }
}
