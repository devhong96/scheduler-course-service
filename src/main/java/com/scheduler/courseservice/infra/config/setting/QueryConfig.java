package com.scheduler.courseservice.infra.config.setting;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.querydsl.jpa.JPQLTemplates.DEFAULT;

@Configuration
@RequiredArgsConstructor
public class QueryConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        {
            return new JPAQueryFactory(DEFAULT, em);
        }
    }
}
