package com.example.Jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String email, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);


        return Jwts.builder()
                .subject(email)
                // Truyền thẳng List<String> vào đây, JJWT sẽ tự động ép kiểu thành Mảng JSON
                .claim("role", roles)
                .expiration(expiryDate)
                .issuedAt(now)
                .signWith(getSignKey()) // Cú pháp chuẩn của bản mới
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryTime = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryTime)
                .signWith(getSignKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token);
            Date now = new Date();
            return true; // Phân tích thành công -> Token hợp lệ
        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }

    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();

    }

    public List<String> extractRoles(String token) {
        // 1. Phân giải token để lấy trọn bộ Payload (Claims)
        Claims claims = Jwts.parser()
                .verifyWith(getSignKey()) // key là SecretKey bạn đã khởi tạo
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 2. Lấy claim có tên "role" và tự động ép kiểu về List
        return claims.get("role", List.class);
    }


}
