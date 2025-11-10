package com.scheduler.courseservice.testSet;

import com.scheduler.courseservice.testSet.messaging.KafkaTestConfig;
import com.scheduler.courseservice.testSet.messaging.TestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@EmbeddedKafka(
        brokerProperties = {
                "listeners=PLAINTEXT://127.0.0.1:0",
                "advertised.listeners=PLAINTEXT://127.0.0.1:0"
        },
        topics = { "course_schedule_logs" }
)
@Import({KafkaTestConfig.class, TestConfig.class})
@ActiveProfiles("test")
public @interface IntegrationTest {

}
