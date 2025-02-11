package com.scheduler.courseservice.infra.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.scheduler.courseservice.infra.log.IPLog.getIpAddress;

@Slf4j
@Aspect
@Component
public class BookLoggingAspect {

    @Around("execution(* com.scheduler.courseservice.*.*.*Controller.*(..))")
    public Object bookController(ProceedingJoinPoint joinPoint) throws Throwable {
        return getObject(joinPoint);
    }

    @Around("execution(* com.scheduler.courseservice.*.*.*Controller.*(..))")
    public Object orderController(ProceedingJoinPoint joinPoint) throws Throwable {
        return getObject(joinPoint);
    }

    private static Object getObject(ProceedingJoinPoint joinPoint) throws Throwable {
        String location = joinPoint.getSignature().getDeclaringTypeName();

        Authentication authentication = SecurityContextHolder
                .getContext().getAuthentication();

        String name = authentication.getName();
        String authority = authentication.getAuthorities().toString();
        String methodName = joinPoint.getSignature().getName();

        try {
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long runtime = endTime - startTime;

            log.info("======================== CONTROLLER EXECUTION START ========================");
            log.info("clientIp = {}", getIpAddress());
            log.info("location = {}", location);
            log.info("username = {}, authority = {}", name, authority);
            log.info("methodName = {}, runtime = {}", methodName, runtime + "ms");
            log.info("dateTime = {}", LocalDateTime.now());
            log.info("======================== CONTROLLER EXECUTION END ========================");

            return result;

        } catch (Throwable e) {
            log.error("======================== ERROR LOGGING STARTED IN CONTROLLER ========================");
            log.error("location = {}", location);
            log.error("username = {}, authority = {}", name, authority);
            log.error("methodName = {}", methodName);
            log.error(e.getMessage());
            log.error("DateTime : {}", LocalDateTime.now());
            log.error("======================== ERROR LOGGING ENDED IN CONTROLLER ========================");
            throw e;
        }
    }
}
