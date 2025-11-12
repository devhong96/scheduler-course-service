package com.scheduler.courseservice.infra.security;

import com.scheduler.courseservice.infra.security.jwt.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public AuthenticationManager authenticationManagerauthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    public static final String[] INTERNAL_ENDPOINTS = {
            "/feign-member-course/**",
    };
    
    public static final String[] ADMIN_ENDPOINTS = {
            "/admin/**"
    };

    public static final String[] TEACHER_ENDPOINTS = {
            "/teacher/**"
    };

    public static final String[] STUDENT_ENDPOINTS = {
            "/student/**"
    };


    public static final String[] ENDPOINTS_WHITELISTS = {
            "/course-api/**",
            "/actuator/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterAt(jwtAuthFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(INTERNAL_ENDPOINTS)
                                .access(
                                        new WebExpressionAuthorizationManager(
                                                "hasIpAddress('127.0.0.1') or hasIpAddress('172.18.0.0/16')")
                                )
                                .requestMatchers(ADMIN_ENDPOINTS).hasAuthority("ADMIN")
                                .requestMatchers(TEACHER_ENDPOINTS).hasAuthority("TEACHER")
                                .requestMatchers(STUDENT_ENDPOINTS).hasAuthority("STUDENT")
                                .requestMatchers(ENDPOINTS_WHITELISTS).permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(STATELESS));
        return httpSecurity.build();
    }
}
