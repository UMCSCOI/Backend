package com.example.scoi.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long verificationTokenExpiration;

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final String VERIFICATION_TOKEN_TYPE = "VERIFICATION";

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.verification-token-expiration}") long verificationTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.verificationTokenExpiration = verificationTokenExpiration;
    }

    public String createAccessToken(String phoneNumber) {
        return createToken(phoneNumber, accessTokenExpiration, ACCESS_TOKEN_TYPE);
    }

    public String createRefreshToken(String phoneNumber) {
        return createToken(phoneNumber, refreshTokenExpiration, REFRESH_TOKEN_TYPE);
    }

    public String createVerificationToken(String phoneNumber) {
        return createToken(phoneNumber, verificationTokenExpiration, VERIFICATION_TOKEN_TYPE);
    }

    private String createToken(String phoneNumber, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(phoneNumber)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | 
                 IllegalArgumentException | ExpiredJwtException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.debug("토큰 만료 확인 중 오류: {}", e.getMessage());
            return true;
        }
    }

    public String getPhoneNumberFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰도 전화번호는 추출 가능 (로그아웃, 재발급 시 필요)
            return e.getClaims().getSubject();
        }
    }

    public long getRemainingTime(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remainingTime, 0); // 음수 방지
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (Exception e) {
            log.debug("남은 시간 계산 중 오류: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Claims 파싱 중복 제거를 위한 private 메서드
     */
    private Claims parseClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    /**
     * Access Token 만료 시간을 초 단위로 반환
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }
}
