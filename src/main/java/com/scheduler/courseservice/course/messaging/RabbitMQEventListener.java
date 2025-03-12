package com.scheduler.courseservice.course.messaging;

import com.rabbitmq.client.Channel;
import com.scheduler.courseservice.course.application.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeStudentName;
import static org.springframework.amqp.support.AmqpHeaders.DELIVERY_TAG;

@Component
@RequiredArgsConstructor
public class RabbitMQEventListener {

    private final CourseService courseService;

    @RabbitListener(queues = "student.name.update.queue", ackMode = "MANUAL")
    public void receiveMessage(
            ChangeStudentName changeStudentName, Channel channel,
            @Header(DELIVERY_TAG) long tag
    ) throws IOException {

        try {
            courseService.changeStudentName(changeStudentName);
            channel.basicAck(tag, false);
        } catch (IOException e) {
            channel.basicNack(tag, false, true);
            throw new RuntimeException(e);
        }
    }
}
