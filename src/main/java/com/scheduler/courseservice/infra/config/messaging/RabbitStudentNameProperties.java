package com.scheduler.courseservice.infra.config.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.rabbitmq.student-name")
@Getter
@Setter
public class RabbitStudentNameProperties {

    private Exchange exchange;
    private Routing routing;
    private Queue queue;

    // 중첩 클래스 Exchange
    @Getter
    @Setter
    public static class Exchange {
        private String name;
        private String dlx;
        private String compensation;

    }

    // 중첩 클래스 Routing
    @Getter
    @Setter
    public static class Routing {
        private String key;
        private String compensation;
    }

    // 중첩 클래스 Queues
    @Getter
    @Setter
    public static class Queue {
        private String name;
        private String dlq;
        private String compensation;
    }

}
