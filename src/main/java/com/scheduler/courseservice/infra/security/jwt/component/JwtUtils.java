package com.scheduler.courseservice.infra.security.jwt.component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;

import static io.jsonwebtoken.io.Decoders.BASE64;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.secretKey}")
    private String secretKey;

    private static SecretKey signingKey;

    @PostConstruct
    public void createSigningKey() {
        byte[] keyBytes = BASE64.decode(secretKey);
        signingKey =  Keys.hmacShaKeyFor(keyBytes);
    }

    @Operation(summary = "인증")
    public Authentication getAuthentication(String token) {
        Claims claims = getPayload(token);

        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Claims getPayload(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
