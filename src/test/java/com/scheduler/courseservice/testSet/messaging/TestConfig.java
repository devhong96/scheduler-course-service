package com.scheduler.courseservice.testSet.messaging;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public WireMockServer wireMockServer() {
        return new WireMockServer(8080);
    }

}
