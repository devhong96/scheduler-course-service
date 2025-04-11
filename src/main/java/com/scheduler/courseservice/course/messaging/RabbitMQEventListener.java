package com.scheduler.courseservice.course.messaging;

import com.rabbitmq.client.Channel;
import com.scheduler.courseservice.course.service.CourseService;
import com.scheduler.courseservice.infra.config.messaging.RabbitStudentNameProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeMemberNameDto;
import static org.springframework.amqp.support.AmqpHeaders.DELIVERY_TAG;

@Component
@RequiredArgsConstructor
public class RabbitMQEventListener {

    private final CourseService courseService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitStudentNameProperties properties;

    @RabbitListener(queues = "${spring.rabbitmq.student-name.queue.name}", ackMode = "MANUAL")
    public void receiveMessage(
            ChangeMemberNameDto changeMemberNameDto, Channel channel,
            @Header(DELIVERY_TAG) long tag
    ) throws IOException {

        try {
            courseService.changeStudentName(changeMemberNameDto);
            channel.basicAck(tag, false);
        } catch (IOException e) {
            channel.basicNack(tag, false, true);

            rabbitTemplate.convertAndSend(
                    properties.getExchange().getName(),
                    properties.getExchange().getCompensation(),
                    changeMemberNameDto);

            throw new RuntimeException(e);
        }
    }
}
