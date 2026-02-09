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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtApiUtil {

    private final MemberApiKeyRepository memberApiKeyRepository;
    private final HashUtil hashUtil;

    /**
     * 업비트 API 통신을 위한 JWT를 생성합니다.
     * 반드시 로그인 된 상태 (JWT이 존재하는 상태) 여야합니다.
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
                .findByMember_PhoneNumberAndExchangeType(phoneNumber, ExchangeType.UPBIT)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // API Key 정보 로깅 (디버깅용)
        log.debug("업비트 API Key 조회 완료 - phoneNumber: {}, publicKey 길이: {}", 
                phoneNumber, apiKey.getPublicKey() != null ? apiKey.getPublicKey().length() : 0);

        String queryHash = getQueryHash(query, body);
        log.debug("업비트 query_hash 생성 완료 - queryHash 길이: {}, isEmpty: {}", 
                queryHash.length(), queryHash.isEmpty());

        // Base64 디코딩 -> AES 복호화 -> 해당 Secret Key로 Signing
        byte[] secretKey = hashUtil.decryptAES(apiKey.getSecretKey());

        Key secret = Keys.hmacShaKeyFor(secretKey);

        String jwt = createUpbitJwt(queryHash, apiKey.getPublicKey(), secret);
        
        // Authorization 헤더 형식 검증
        // 중요: "Bearer " (공백 포함) 형식이어야 하며, 개행 문자나 추가 공백이 없어야 함
        String authorization = "Bearer " + jwt.trim();
        
        // 형식 검증
        if (!authorization.startsWith("Bearer ")) {
            log.error("⚠️ Authorization 헤더 형식 오류 - 'Bearer '로 시작하지 않음");
            throw new GeneralSecurityException("Authorization 헤더 형식 오류");
        }
        if (authorization.contains("\n") || authorization.contains("\r")) {
            log.error("⚠️ Authorization 헤더에 개행 문자가 포함됨!");
            throw new GeneralSecurityException("Authorization 헤더에 개행 문자 포함");
        }
        if (authorization.length() <= 7) {
            log.error("⚠️ Authorization 헤더가 너무 짧음: {}", authorization);
            throw new GeneralSecurityException("Authorization 헤더 형식 오류");
        }
        
        log.debug("업비트 JWT 생성 완료 - phoneNumber: {}, queryHash isEmpty: {}, authorization 길이: {}", 
                phoneNumber, queryHash.isEmpty(), authorization.length());
        
        return authorization;
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
                .findByMember_PhoneNumberAndExchangeType(phoneNumber, ExchangeType.BITHUMB)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // query SHA512 암호화
        String queryHash = getQueryHash(query, body);

        byte[] secretKey = hashUtil.decryptAES(apiKey.getSecretKey());

        Key secret = Keys.hmacShaKeyFor(secretKey);

        return "Bearer " + createBithumbJwt(queryHash, apiKey.getPublicKey(), secret);
    }

    /**
     * API키를 테스트하기 위한 JWT를 생성합니다.
     * @param publicKey 퍼블릭키
     * @param secretKey 시크릿키
     * @return JWT를 반환합니다.
     * @throws GeneralSecurityException JWT 변환에 실패했을 경우
     * @throws IllegalArgumentException 거래소 타입이 잘못된 경우
     */
    public String createJwtWithApiKeys(
            @NotNull String publicKey,
            @NotNull String secretKey,
            @NotNull ExchangeType exchangeType
    ) throws GeneralSecurityException, IllegalArgumentException{

        byte[] decryptedSecretKey = hashUtil.decryptAES(secretKey);

        Key secret = Keys.hmacShaKeyFor(decryptedSecretKey);

        switch (exchangeType){
            case UPBIT -> {
                return "Bearer "+createUpbitJwt("",publicKey,secret);
            }
            case BITHUMB -> {
                return "Bearer "+createBithumbJwt("",publicKey,secret);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    // query SHA512 암호화
    // 업비트 API 규격:
    // - GET /v1/accounts (query 없음) → query_hash 없음
    // - GET /v1/orders/chance?market=KRW-BTC (query 있음) → query_hash 필요
    // - POST /v1/orders (body 있음) → query_hash 필요
    private String getQueryHash(
            String query,
            Record body
    ) throws NoSuchAlgorithmException {

        String queryHash = "";
        
        // 1. GET 요청의 query 파라미터가 있는 경우
        if (query != null && !query.isEmpty()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            log.debug("업비트 query_hash 계산 - query 파라미터: {}", query);
            digest.update(query.getBytes(StandardCharsets.UTF_8));
            queryHash = HexFormat.of().formatHex(digest.digest());
            log.debug("업비트 query_hash 계산 - query hash: {}", queryHash);
        }
        
        // 2. POST/PUT/DELETE 요청의 body가 있는 경우
        // 🔥 중요: 업비트는 "실제로 보낸 요청 내용 그대로"를 해시해야 함
        // query_hash = SHA512(실제 전송되는 JSON body를 query string으로 변환한 것)
        if (body != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            ObjectMapper objectMapper = new ObjectMapper();
            // null 필드 제외 설정 (업비트 규격)
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            
            try {
                // 1. body를 JSON으로 직렬화 (Feign Client가 실제로 보내는 형식과 동일)
                String jsonBody = objectMapper.writeValueAsString(body);
                log.info("업비트 query_hash 계산 - 실제 전송되는 JSON body: {}", jsonBody);
                
                // 2. JSON을 LinkedHashMap으로 파싱하여 순서 보장
                // 업비트 API는 JSON 파싱 순서 그대로 query string을 생성
                LinkedHashMap<String, Object> map = objectMapper.readValue(jsonBody, 
                        objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));

                StringBuilder result = new StringBuilder();

                // 3. Map을 순회하여 query string 생성 (순서 유지, 정렬하지 않음)
                // 중요: 숫자 타입이 변경되지 않도록 주의 (100 -> "100", 0.001 -> "0.001")
                for (String key : map.keySet()) {
                    Object value = map.get(key);
                    // null 값과 빈 문자열은 제외
                    if (value != null && !value.toString().isEmpty()) {
                        // 숫자 타입 처리: Integer는 "100", Double은 "0.001" 형식 유지
                        String valueStr;
                        if (value instanceof Number) {
                            // 숫자는 그대로 문자열로 변환 (소수점 형식 유지)
                            valueStr = value.toString();
                        } else {
                            valueStr = value.toString();
                        }
                        result.append(key).append("=").append(valueStr).append("&");
                    } else {
                        log.debug("업비트 query_hash 계산에서 제외된 필드 - key: {}, value: {}", key, value);
                    }
                }
                
                if (result.length() > 0) {
                    result.deleteCharAt(result.length() - 1);
                }

                String queryString = result.toString();
                log.info("업비트 query_hash 계산 - 생성된 query string: {}", queryString);
                
                // 4. query string을 UTF-8 바이트로 변환하여 SHA-512 해시
                digest.update(queryString.getBytes(StandardCharsets.UTF_8));
                queryHash = HexFormat.of().formatHex(digest.digest());
                log.info("업비트 query_hash 계산 - 최종 hash: {}", queryHash);
            } catch (Exception e) {
                log.error("Query Hash 생성 실패: {}", e.getMessage(), e);
                throw new NoSuchAlgorithmException("Query Hash 생성 실패: " + e.getMessage(), e);
            }
        }
        return queryHash;
    }

    private String createBithumbJwt(
            String queryHash,
            String publicKey,
            Key secretKey
    ){
        // 빗썸 규격: nonce 문자열 변환 및 query_hash_alg 추가
        if (queryHash.isEmpty()){
            return Jwts.builder()
                    .claim("access_key", publicKey)
                    .claim("nonce", UUID.randomUUID().toString())
                    .claim("timestamp", System.currentTimeMillis())
                    .signWith(secretKey)
                    .compact();
        } else {
            return Jwts.builder()
                    .claim("access_key", publicKey)
                    .claim("nonce", UUID.randomUUID().toString())
                    .claim("timestamp", System.currentTimeMillis())
                    .claim("query_hash", queryHash)
                    .claim("query_hash_alg", "SHA512")
                    .signWith(secretKey)
                    .compact();
        }
    }

    private String createUpbitJwt(
            @NotNull String queryHash,
            @NotNull String publicKey,
            @NotNull Key secretKey
    ){
        // 업비트 규격에 따라 query_hash가 있을 때만 포함
        // GET /v1/accounts 같은 query 없는 요청은 query_hash를 넣지 않음
        if (queryHash.isEmpty()){
            // query_hash 없음 - access_key와 nonce만 포함
            log.debug("업비트 JWT 생성 - query_hash 없음 (GET /accounts 같은 요청)");
            return Jwts.builder()
                    .header().add("typ","JWT")
                    .and()
                    .claim("access_key", publicKey)
                    .claim("nonce", UUID.randomUUID().toString())
                    .signWith(secretKey)
                    .compact();
        } else {
            // query_hash 있음 - POST /orders 같은 요청
            log.debug("업비트 JWT 생성 - query_hash 포함: {}", queryHash.substring(0, Math.min(16, queryHash.length())));
            return Jwts.builder()
                    .header().add("typ","JWT")
                    .and()
                    .claim("access_key", publicKey)
                    .claim("nonce", UUID.randomUUID().toString())
                    .claim("query_hash", queryHash)
                    .claim("query_hash_alg", "SHA512")
                    .signWith(secretKey)
                    .compact();
        }
    }
}