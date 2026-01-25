package com.example.scoi.global.util;

import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.repository.MemberApiKeyRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtApiUtil {

    private final MemberApiKeyRepository memberApiKeyRepository;

    private static final String ALGORITHM = "AES";

    @Value("${jwt.key}")
    private String key;

    /**
     * 업비트 API 통신을 위한 JWT를 생성합니다.
     * GET 요청은 파라미터를 (ex. key=abc&key2[]=abc&key2[]=abc)
     * 그 외 Request Body가 들어있는 요청은 body에 DTO(Record) 그대로 넣어주시면 됩니다.
     * @param phoneNumber 사용자의 휴대전화 번호
     * @param query GET 요청은 필수 기입
     * @param body POST, PUT, DELETE는 필수 기입
     * @return Bearer {JWT토큰}
     */
    public String createUpBitJwt(
            @NotNull String phoneNumber,
            @Nullable String query,
            @Nullable Record body
    ) throws GeneralSecurityException {

        // API키 객체 찾기
        MemberApiKey apiKey = memberApiKeyRepository
                .findByMemberPhoneNumberAndExchangeType(phoneNumber, ExchangeType.UPBIT)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        String queryHash = getQueryHash(query, body);

        // 시크릿키 키 세팅 (복호화)
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey aesKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);

        // Base64 디코딩 -> AES 복호화 -> 해당 Secret Key로 Signing
        byte[] secretKey = cipher.doFinal(Base64.getDecoder().decode(apiKey.getSecretKey()));

        String jwt;
        Key secret = Keys.hmacShaKeyFor(secretKey);

        // JWT 생성
        // Query Parameter 혹은 Request Body가 없는 경우 -> query_hash 빼고 생성
        if (queryHash.isEmpty()){
            jwt = Jwts.builder()
                    .header().add("typ","JWT")
                    .and()
                    .claim("access_key", apiKey.getPublicKey())
                    .claim("nonce", UUID.randomUUID())
                    .signWith(secret)
                    .compact();
        } else {
            jwt = Jwts.builder()
                    .header().add("typ","JWT")
                    .and()
                    .claim("access_key", apiKey.getPublicKey())
                    .claim("nonce", UUID.randomUUID())
                    .claim("query_hash", queryHash)
                    .signWith(secret)
                    .compact();
        }

        return "Bearer "+jwt;
    }

    /**
     * 빗썸 API 통신을 위한 JWT를 생성합니다.
     * 반드시 로그인 된 상태 (JWT이 존재하는 상태) 여야합니다.
     * GET 요청은 파라미터를 (ex. key=abc&key2[]=abc&key2[]=abc)
     * 그 외 Request Body가 들어있는 요청은 body에 DTO(Record) 형태로 넣어주시면 됩니다.
     * @param phoneNumber 사용자의 휴대전화 번호
     * @param query GET 요청은 필수 기입
     * @param body POST, PUT, DELETE는 필수 기입
     * @return Bearer {JWT토큰}
     */
    public String createBithumbJwt(
            @NotNull String phoneNumber,
            @Nullable String query,
            @Nullable Record body
    ) throws GeneralSecurityException {

        // API키 객체 찾기
        MemberApiKey apiKey = memberApiKeyRepository
                .findByMemberPhoneNumberAndExchangeType(phoneNumber, ExchangeType.BITHUMB)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        // query SHA512 암호화
        String queryHash = getQueryHash(query, body);

        // 시크릿키 키 세팅 (복호화)
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey aesKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);

        // Base64 디코딩 -> AES 복호화 -> 해당 Secret Key로 Signing
        byte[] secretKey = cipher.doFinal(Base64.getDecoder().decode(apiKey.getSecretKey()));

        String jwt;
        Key secret = Keys.hmacShaKeyFor(secretKey);

        // JWT 생성
        // Query Parameter 혹은 Request Body가 없는 경우 -> query_hash 빼고 생성
        if (queryHash.isEmpty()){
            jwt = Jwts.builder()
                    .claim("access_key", apiKey.getPublicKey())
                    .claim("nonce", UUID.randomUUID())
                    .claim("timestamp", System.currentTimeMillis())
                    .signWith(secret)
                    .compact();
        } else {
            jwt = Jwts.builder()
                    .claim("access_key", apiKey.getPublicKey())
                    .claim("nonce", UUID.randomUUID())
                    .claim("timestamp", System.currentTimeMillis())
                    .claim("query_hash", queryHash)
                    .signWith(secret)
                    .compact();
        }

        return "Bearer "+jwt;
    }

    // query SHA512 암호화
    private String getQueryHash(
            String query,
            Record body
    ) throws NoSuchAlgorithmException {

        String queryHash = "";
        if (query != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(query.getBytes());
            queryHash = HexFormat.of().formatHex(digest.digest());
        }
        if (body != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.convertValue(body, Map.class);

            StringBuilder result = new StringBuilder();

            for (String key : map.keySet()) {
                result.append(key).append("=").append(map.get(key)).append("&");
            }
            result.deleteCharAt(result.length() - 1);

            digest.update(String.valueOf(result).getBytes());
            queryHash = HexFormat.of().formatHex(digest.digest());
        }
        return queryHash;
    }
}
