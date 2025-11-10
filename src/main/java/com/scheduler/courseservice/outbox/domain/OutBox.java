package com.scheduler.courseservice.outbox.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@DynamicUpdate
@NoArgsConstructor(access = PROTECTED)
public class OutBox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(unique = true)
    private String idempotency;

    @Enumerated(STRING)
    private EventType eventType;

    @Lob
    private String payload;

    public static OutBox create(EventType eventType, String payload) {
        OutBox outBox = new OutBox();
        outBox.eventType = eventType;
        outBox.payload = payload;
        return outBox;
    }

    @PrePersist
    void createKey() {
        idempotency = UUID.randomUUID().toString();
    }
}
