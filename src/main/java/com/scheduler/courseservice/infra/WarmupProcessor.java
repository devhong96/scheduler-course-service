package com.scheduler.courseservice.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class WarmupProcessor implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    public WarmupProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        // 모든 빈 검색
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);

            if (bean.getClass().isAnnotationPresent(Warmup.class)) {
                log.info("Warming up bean: {}", beanName);
            }

            for (Method method : bean.getClass().getMethods()) {
                if (method.isAnnotationPresent(Warmup.class)) {
                    try {
                        log.info("Warming up method: {} in bean: {}", method.getName(), beanName);
                        method.invoke(bean);
                    } catch (Exception e) {
                        log.warn("Failed to warm up method: {}", method.getName());
                        log.warn("Failed to warm up method: {}", e.getMessage());
                    }
                }
            }
        }
    }
}