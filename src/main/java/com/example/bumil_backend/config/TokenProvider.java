package com.example.bumil_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenProvider {
    private final SecretKey key;

    private static final long ACCESS_TOKEN_ABILITY = 1000L * 60 * 5;
    private static final long REFRESH_TOKEN_ABILITY = 1000L * 60 * 60 * 24;

    public TokenProvider(@Value("${spring.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //액세스 토큰 생성
    public String createAccessToken(String username, String role){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_ABILITY);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    //리프레시 토큰 생성
    public String createRefreshToken(String username, String role){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_ABILITY);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    //사용자 정보 추출
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    //사용자 역할 추출
    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    //토큰 유효성 검사
    public boolean validateToken(String token){
        try{
            Jwts.parser()
                    .verifyWith(this.key) // SecretKey
                    .build()
                    .parseSignedClaims(token); // 서명된 클레임 파싱 및 검증
            return true;
        } catch(JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
