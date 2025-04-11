package com.scheduler.courseservice.infra.config.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RabbitTeacherNameConfig {

    private final RabbitTeacherNameProperties properties;

    @Bean
    public DirectExchange teacherNameExchange() {
        return new DirectExchange(properties.getExchange().getName());
    }

    @Bean
    public Binding teacherNameBinding(Queue teacherNameQueue, DirectExchange teacherNameExchange) {
        return BindingBuilder.bind(teacherNameQueue).to(teacherNameExchange).with(properties.getRouting().getKey());
    }

    @Bean
    public Queue teacherNameQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", properties.getExchange().getDlx());
        args.put("x-dead-letter-routing-key", properties.getRouting().getKey());
        return new Queue(properties.getQueue().getName(), true, false, false, args);
    }

    @Bean
    public DirectExchange teacherNameDLX() {
        return new DirectExchange(properties.getExchange().getDlx());
    }

    @Bean
    public Queue teacherNameDLQ() {
        return new Queue(properties.getQueue().getDlq(), true);
    }

    @Bean
    public Binding teacherNameDLB(Queue teacherNameDLQ, DirectExchange teacherNameDLX) {
        return BindingBuilder.bind(teacherNameDLQ).to(teacherNameDLX).with(properties.getQueue().getDlq());
    }

    @Bean
    public DirectExchange teacherNameCE() {
        return new DirectExchange(properties.getExchange().getCompensation());
    }

    @Bean
    public Queue teacherNameCQ() {
        return new Queue(properties.getQueue().getCompensation(), true);
    }

    @Bean
    public Binding teacherNameCB(Queue teacherNameCQ, DirectExchange teacherNameCE) {
        return BindingBuilder.bind(teacherNameCQ).to(teacherNameCE)
                .with(properties.getRouting().getCompensation());
    }
}