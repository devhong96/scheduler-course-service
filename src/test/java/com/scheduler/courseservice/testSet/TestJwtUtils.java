package com.scheduler.courseservice.testSet;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

import static io.jsonwebtoken.io.Decoders.BASE64;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestJwtUtils {

    @Value("${jwt.secret_key}")
    private String secretKey;

    @PostConstruct
    public void createSigningKey() {
        byte[] keyBytes = BASE64.decode(secretKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Getter
    private SecretKey signingKey; // static 제거

    private static final Long accessTokenPeriod = 60L * 60L * 12L; // 12시간
    private static final Long refreshTokenPeriod = 60L * 60L * 24L * 30L; // 24시간


    @Operation(summary = "토큰 생성", description = "계정 이름과 권한을 토큰에 저장")
    public JwtTokenDto generateToken(Authentication authentication) {

        String username = authentication.getName();
        String auth = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","))
                .substring(5);

        return new JwtTokenDto(
                generateToken(username, auth, "access", accessTokenPeriod),
                generateToken(username, auth, "refresh", refreshTokenPeriod),
                Date.from(Instant.now().plusSeconds(refreshTokenPeriod))
        );
    }

    private String generateToken(String username, String auth, String category, long tokenPeriod) {
        return Jwts.builder()
                .subject(username)
                .claim("auth", auth)
                .claim("category", category)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(tokenPeriod)))
                .signWith(signingKey)
                .compact();
    }

}
