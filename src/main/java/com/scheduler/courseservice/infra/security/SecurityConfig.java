package com.scheduler.courseservice.infra.security;

import com.scheduler.courseservice.infra.security.jwt.component.JwtUtils;
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

    private final JwtUtils jwtUtils;

    @Bean
    public AuthenticationManager authenticationManagerauthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    public static final String[] INTERNAL_ENDPOINTS = {
            "/feign-member-ebook/**",
            "/feign-order-ebook/**",
            "/actuator/**",

            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",

    };
    
    public static final String[] ADMIN_RESTRICTED_ENDPOINTS = {

    };

    public static final String[] RESTRICTED_ENDPOINTS = {

    };

    public static final String[] AUTHORIZED_ENDPOINTS = {

    };

    public static final String[] ENDPOINTS_WHITELISTS = {

    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterAt(new JwtAuthFilter(jwtUtils), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(
        auth -> auth
                .requestMatchers(INTERNAL_ENDPOINTS)
                .access(
                        new WebExpressionAuthorizationManager(
                                "hasIpAddress('127.0.0.1') or hasIpAddress('172.18.0.0/16')")
                )
                .requestMatchers(ADMIN_RESTRICTED_ENDPOINTS).hasAuthority("ADMIN")
                .requestMatchers(RESTRICTED_ENDPOINTS).hasAnyAuthority("ADMIN", "TEACHER")
                .requestMatchers(AUTHORIZED_ENDPOINTS).hasAnyAuthority("ADMIN", "TEACHER", "READER")
                .requestMatchers(ENDPOINTS_WHITELISTS).permitAll()
                .anyRequest().authenticated()
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(STATELESS));

        return httpSecurity.build();
    }
}
