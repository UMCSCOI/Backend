package com.example.scoi.domain.charge.service;

import com.example.scoi.domain.charge.converter.ChargeConverter;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.enums.DepositType;
import com.example.scoi.domain.charge.enums.MFAType;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.repository.MemberApiKeyRepository;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.global.client.BithumbClient;
import com.example.scoi.global.client.UpbitClient;
import com.example.scoi.global.client.converter.BithumbConverter;
import com.example.scoi.global.client.converter.UpbitConverter;
import com.example.scoi.global.client.dto.*;
import com.example.scoi.global.util.JwtApiUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChargeService {

    private final JwtApiUtil jwtApiUtil;
    private final BithumbClient bithumbClient;
    private final UpbitClient upbitClient;
    private final MemberRepository memberRepository;
    private final MemberApiKeyRepository memberApiKeyRepository;

    // 원화 충전 요청하기
    public ChargeResDTO.ChargeKrw chargeKrw(
            String phoneNumber,
            ChargeReqDTO.ChargeKrw dto
    ) {
        // 거래소별 지원 2차 인증서인지 확인
        // 벗썸만 카카오 단독 지원
        if (dto.exchangeType().equals(ExchangeType.BITHUMB) && !dto.MFA().equals(MFAType.KAKAO)){
            throw new ChargeException(ChargeErrorCode.INVALIDED_TWO_FACTOR_AUTH);
        }
        // 거래소별 분기
        String token;
        String uuid, txid;
        try {
            switch (dto.exchangeType()) {
                case UPBIT :
                    UpbitReqDTO.ChargeKrw upbitDto = UpbitConverter.toChargeKrw(dto);
                    token = jwtApiUtil.createUpBitJwt(phoneNumber,null, upbitDto);
                    UpbitResDTO.ChargeKrw upbitResult = upbitClient.chargeKrw(token,upbitDto);
                    uuid = upbitResult.uuid();
                    txid = upbitResult.txid();
                    break;
                case BITHUMB :
                    BithumbReqDTO.ChargeKrw bithumbDto = BithumbConverter.toChargeKrw(dto);
                    token = jwtApiUtil.createBithumbJwt(phoneNumber, null, bithumbDto);
                    BithumbResDTO.ChargeKrw bithumbResult = bithumbClient.chargeKrw(token,bithumbDto);
                    uuid = bithumbResult.uuid();
                    txid = bithumbResult.txid();
                    break;
                default:
                    throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
            }
        // 토큰 못 만들었을 경우
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        // 거래소 400 BadRequest
        } catch (FeignException.BadRequest e) {

            // Error 변환
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            switch (error.error().name()){
                // 2차 인증서 문제
                case "validation_error":
                case "two_factor_auth_required":
                    throw new ChargeException(ChargeErrorCode.TWO_FACTOR_AUTH_REQUIRED);
                // 최소 주문
                case "deposit_amount_too_small":
                    throw new ChargeException(ChargeErrorCode.MINIMUM_DEPOSIT_BAD_REQUEST);
            }

            // 나머지 400 에러
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);

        } catch (FeignException.Unauthorized e) {

            // Error 변환
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 권한 부족
            if (error.error().name().equals("out_of_scope")){
                throw new ChargeException(ChargeErrorCode.EXCHANGE_FORBIDDEN);
            }

            // 그 이외에는 JWT 관련 오류
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }

        // 정상 처리되었을때
        return ChargeConverter.toChargeKrw(uuid,txid);
    }

    // 특정 주문 확인하기
    public String getOrders(
            String phoneNumber,
            ChargeReqDTO.GetOrder dto
    ) {
        // 거래소별 분기
        String token;
        String result;
        try {
            if (dto.depositType().equals(DepositType.ORDER)){
                switch (dto.exchangeType()){
                    case UPBIT:
                        token = jwtApiUtil.createUpBitJwt(phoneNumber, "uuid="+dto.uuid(), null);
                        UpbitResDTO.GetOrder upbitResult = upbitClient.getOrder(token, dto.uuid());
                        result = upbitResult.state();
                        break;
                    case BITHUMB:
                        token = jwtApiUtil.createBithumbJwt(phoneNumber, "uuid="+dto.uuid(), null);
                        BithumbResDTO.GetOrder bithumbResult = bithumbClient.getOrder(token, dto.uuid());
                        result = bithumbResult.state();
                        break;
                    default:
                        throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
                }
            } else if (dto.depositType().equals(DepositType.DEPOSIT)) {
                switch (dto.exchangeType()) {
                    case UPBIT:
                        token = jwtApiUtil.createUpBitJwt(phoneNumber, "uuid="+dto.uuid()+"&currency=KRW", null);
                        UpbitResDTO.GetDeposit upbitResult = upbitClient.getDeposit(token, dto.uuid(), "KRW");
                        result = upbitResult.state();
                        break;
                    case BITHUMB:
                        token = jwtApiUtil.createBithumbJwt(phoneNumber, "uuid="+dto.uuid()+"&currency=KRW", null);
                        BithumbResDTO.GetDeposit bithumbResult = bithumbClient.getDeposit(token, dto.uuid(), "KRW");
                        // PROCESSING, REJECTED, ACCEPTED
                        if (bithumbResult.state().equals("REJECTED")){
                            result = "CANCELLED";
                        } else {
                            result = bithumbResult.state();
                        }
                        break;
                    default:
                        throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
                }
            } else {
                throw new ChargeException(ChargeErrorCode.WRONG_DEPOSIT_TYPE);
            }
        // 토큰 못 만들었을 경우
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 주문을 찾지 못한 경우
            if (error.error().name().equals("order_not_found")) {
                throw new ChargeException(ChargeErrorCode.ORDER_NOT_FOUND);
            }

            // 나머지 400 에러
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        } catch (FeignException.Unauthorized e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 권한이 부족한 경우
            if (error.error().name().equals("out_of_scope")) {
                throw new ChargeException(ChargeErrorCode.EXCHANGE_FORBIDDEN);
            }

            // 나머지 JWT 관련 오류
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }

        return result.toUpperCase();
    }

    //보유 자산 조회
     
    public BalanceResDTO.BalanceListDTO getBalancesByPhone(String phoneNumber, ExchangeType exchangeType) {
        log.info("getBalancesByPhone 호출 - phoneNumber: {}, exchangeType: {}", phoneNumber, exchangeType);
        try {
            List<BalanceResDTO.BalanceDTO> balances;
            
            switch (exchangeType) {
                case UPBIT:
                    String jwt = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
                    // 디버깅: Authorization 헤더 형식 확인
                    log.debug("ChargeService - 업비트 API 호출 - phoneNumber: {}, authorization 시작: {}",
                            phoneNumber, jwt.substring(0, Math.min(30, jwt.length())));
                    if (!jwt.startsWith("Bearer ")) {
                        log.error("ChargeService - Authorization 헤더에 'Bearer '가 없습니다! - phoneNumber: {}, jwt: {}",
                                phoneNumber, jwt.substring(0, Math.min(50, jwt.length())));
                    }
                    UpbitResDTO.BalanceResponse[] upbitResponses = upbitClient.getAccount(jwt);
                    balances = UpbitConverter.toBalanceDTOList(upbitResponses);
                    break;
                case BITHUMB:
                    jwt = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
                    BithumbResDTO.BalanceResponse[] bithumbResponses = bithumbClient.getAccount(jwt);
                    balances = BithumbConverter.toBalanceDTOList(bithumbResponses);
                    break;
                default:
                    throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
            }
            
            return BalanceResDTO.BalanceListDTO.builder()
                    .balances(balances)
                    .build();
        } catch (MemberException e) {
            log.error("ChargeService - 업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("ChargeService - 보유자산 조회 JWT 생성 실패", e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    // API 키를 찾을 수 없는 경우
                    if (e instanceof FeignException.NotFound) {
                        throw new ChargeException(ChargeErrorCode.EXCHANGE_API_KEY_NOT_FOUND);
                    }

                    // 나머지 400 에러
                    throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("ChargeService - 보유자산 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("ChargeService - 보유자산 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException.Unauthorized e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    // 권한이 부족한 경우
                    if (error.error().name().equals("out_of_scope")) {
                        throw new ChargeException(ChargeErrorCode.EXCHANGE_FORBIDDEN);
                    }

                    // 나머지 JWT 관련 오류
                    throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("ChargeService - 보유자산 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("ChargeService - 보유자산 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException e) {
            // 다른 FeignException 타입들(Forbidden, InternalServerError 등)은 그대로 전파
            log.error("ChargeService - 보유자산 조회 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
        } catch (Exception e) {
            // FeignException이 아닌 경우에만 ChargeException으로 변환
            log.error("ChargeService - 보유자산 조회 API 호출 실패", e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    // 입금 주소 확인하기
    public String getDepositAddress(
            String phoneNumber,
            ExchangeType exchangeType
    ) {

        // 거래소별 요청 보내기
        String token;
        String result;
        try{
            switch (exchangeType) {
                case UPBIT:
                    token = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
                    List<UpbitResDTO.GetDepositAddress> upbitResult = upbitClient.getDepositAddresses(token);

                    // 만약 해당 유저 입금 주소가 존재하지 않을 때: 업비트는 빈 리스트 반환
                    if (upbitResult.isEmpty()) {
                        throw new ChargeException(ChargeErrorCode.ADDRESS_NOT_FOUND);
                    }

                    result = upbitResult.getFirst().deposit_address();
                    break;
                case BITHUMB:
                    token = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
                    List<BithumbResDTO.GetDepositAddress> bithumbResult = bithumbClient.getDepositAddresses(token);

                    // 만약 해당 유저 입금 주소가 존재하지 않을 때 (추측)
                    if (bithumbResult.isEmpty()) {
                        throw new ChargeException(ChargeErrorCode.ADDRESS_NOT_FOUND);
                    }

                    result = bithumbResult.getFirst().deposit_address();
                    break;
                default:
                    throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
            }
        // 거래소 JWT 토큰 오류
        } catch (GeneralSecurityException e) {
            throw new ChargeException(ChargeErrorCode.EXCHANGE_API_KEY_NOT_FOUND);
        // 거래소 JWT 토큰 인증 오류
        } catch (FeignException.Unauthorized e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            if (error.error().name().equals("out_of_scope")) {
                throw new ChargeException(ChargeErrorCode.EXCHANGE_FORBIDDEN);
            }
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        // 거래소별 입금 주소가 없는 경우 (추측)
        } catch (FeignException.NotFound e) {
            throw new ChargeException(ChargeErrorCode.ADDRESS_NOT_FOUND);
        }

        return result;
    }

    // 입금 주소 생성하기
    public List<String> createDepositAddress(
            String phoneNumber,
            ChargeReqDTO.CreateDepositAddress dto
    ) {

        // 거래소별 요청 보내기
        String token;
        List<String> result = new ArrayList<>();
        try{
            switch (dto.exchangeType()){
                case UPBIT:
                    for (int idx = 0; idx < dto.coinType().size(); idx++) {
                        // 소문자로 변경
                        String coin = dto.coinType().get(idx).toUpperCase();
                        String netType = dto.netType().get(idx).toUpperCase();

                        UpbitReqDTO.CreateDepositAddress upbitDto = UpbitConverter
                                .toCreateDepositAddress(coin, netType);

                        token = jwtApiUtil
                                .createUpBitJwt(phoneNumber, null, upbitDto);

                        UpbitResDTO.CreateDepositAddress upbitResult = upbitClient
                                .createDepositAddress(token, upbitDto);

                        result.add(coin);
                    }
                    break;
                case BITHUMB:
                    for (int idx = 0; idx < dto.coinType().size(); idx++) {
                        // 소문자로 변경
                        String coin = dto.coinType().get(idx).toUpperCase();
                        String netType = dto.netType().get(idx).toUpperCase();

                        BithumbReqDTO.CreateDepositAddress bithumbDto = BithumbConverter
                                .toCreateDepositAddress(coin, netType);

                        token = jwtApiUtil
                                .createBithumbJwt(phoneNumber, null, bithumbDto);

                        BithumbResDTO.CreateDepositAddress bithumbResult = bithumbClient
                                .createDepositAddress(token, bithumbDto);

                        result.add(coin);
                    }
                    break;
                default:
                    throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
            }
        // JWT 못 만들었을 경우
        } catch (GeneralSecurityException e) {
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        // 파라미터 빼먹은 경우
        } catch (FeignException.BadRequest e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Error error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class).error();

            if (error.name().equals("invalid_parameter")){
                throw new ChargeException(ChargeErrorCode.WRONG_COIN_TYPE);
            }

            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }

        return result;
    }
}
