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
public class RabbitStudentNameConfig {

    private final RabbitStudentNameProperties properties;

    @Bean
    public DirectExchange studentNameExchange() {
        return new DirectExchange(properties.getExchange().getName());
    }

    @Bean
    public Binding studentNameBinding(Queue studentNameQueue, DirectExchange studentNameExchange) {
        return BindingBuilder.bind(studentNameQueue).to(studentNameExchange).with(properties.getRouting().getKey());
    }

    @Bean
    public Queue studentNameQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", properties.getExchange().getDlx());
        args.put("x-dead-letter-routing-key", properties.getRouting().getKey());
        return new Queue(properties.getQueue().getName(), true, false, false, args);
    }

    @Bean
    public DirectExchange studentNameDLX() {
        return new DirectExchange(properties.getExchange().getDlx());
    }

    @Bean
    public Queue studentNameDLQ() {
        return new Queue(properties.getQueue().getDlq(), true);
    }

    @Bean
    public Binding studentNameDLB(Queue studentNameDLQ, DirectExchange studentNameDLX) {
        return BindingBuilder.bind(studentNameDLQ).to(studentNameDLX).with(properties.getQueue().getDlq());
    }

    @Bean
    public DirectExchange studentNameCE() {
        return new DirectExchange(properties.getExchange().getCompensation());
    }

    @Bean
    public Queue studentNameCQ() {
        return new Queue(properties.getQueue().getCompensation(), true);
    }

    @Bean
    public Binding studentNameCB(Queue studentNameCQ, DirectExchange studentNameCE) {
        return BindingBuilder.bind(studentNameCQ).to(studentNameCE)
                .with(properties.getRouting().getCompensation());
    }

}