package com.scheduler.courseservice.infra.config.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> stringObjectMap = kafkaProperties.getProducer().buildProperties(null);
        stringObjectMap.putIfAbsent(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new DefaultKafkaProducerFactory<>(stringObjectMap);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> stringObjectMap = kafkaProperties.getConsumer().buildProperties(null);
        stringObjectMap.putIfAbsent(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(stringObjectMap);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // 컨슈머가 오류로 인해 중단될 경우, 자동으로 다시 시작하도록 설정
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(5000L, 3)); // 5초 간격으로 최대 3번 재시도
        factory.setCommonErrorHandler(errorHandler);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        return executor;
    }

    @Bean
    public Executor messageRelayPublishPendingEventExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

}
