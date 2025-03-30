package com.scheduler.courseservice.testSet.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

@TestConfiguration
@RequiredArgsConstructor
public class KafkaTestConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, String> testProducerFactory() {
        Map<String, Object> stringObjectMap = kafkaProperties.getProducer().buildProperties(null);
        stringObjectMap.putIfAbsent(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new DefaultKafkaProducerFactory<>(stringObjectMap);
    }

    @Bean
    public KafkaTemplate<String, String> testKafkaTemplate() {
        return new KafkaTemplate<>(testProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> testConsumerFactory() {
        Map<String, Object> stringObjectMap = kafkaProperties.getConsumer().buildProperties(null);
        stringObjectMap.putIfAbsent(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(stringObjectMap);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> testKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(testConsumerFactory());

        // 컨슈머가 오류로 인해 중단될 경우, 자동으로 다시 시작하도록 설정
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(5000L, 3)); // 5초 간격으로 최대 3번 재시도
        factory.setCommonErrorHandler(errorHandler);
        factory.setBatchListener(true);
        return factory;
    }

}
