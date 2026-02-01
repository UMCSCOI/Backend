package com.example.scoi.domain.charge.service;

import com.example.scoi.domain.charge.converter.ChargeConverter;
import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.charge.enums.MFAType;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.global.client.BithumbClient;
import com.example.scoi.global.client.UpbitClient;
import com.example.scoi.global.client.adapter.BithumbAdapter;
import com.example.scoi.global.client.adapter.UpbitAdapter;
import com.example.scoi.global.client.adapter.ExchangeApiClient;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChargeService {

    private final JwtApiUtil jwtApiUtil;
    private final BithumbClient bithumbClient;
    private final UpbitClient upbitClient;
    private final MemberRepository memberRepository;
    private final BithumbAdapter bithumbAdapter;
    private final UpbitAdapter upbitAdapter;

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

    /**
     * 보유 자산 조회
     * 지금 버전: Member 조회 후 phoneNumber 사용
     * dev로 최신화 후 병합하면 AuthUser 구현 후 파라미터를 AuthUser로 변경 예정
     */
    public BalanceResDTO.BalanceDTO getBalances(Long memberId, ExchangeType exchangeType) {
        // 사용자의 API 키를 DB에서 가져오기 (Member 조회하여 phoneNumber 가져오기)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ChargeException(ChargeErrorCode.EXCHANGE_API_KEY_NOT_FOUND));

        String phoneNumber = member.getPhoneNumber();

        // 시크릿 키 복호화하기 -JwtApiUtil
        //쿼리 파라미터에 따라 빗썸 /업비트 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);

        try {//응답
            return apiClient.getBalance(phoneNumber, exchangeType);
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}, error: {}",
                    exchangeType, phoneNumber, e.getMessage(), e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    /**
     * 보유 자산 조회 (phone 파라미터로 직접 조회)
     * 테스트용
     */
    public BalanceResDTO.BalanceDTO getBalancesByPhone(String phoneNumber, ExchangeType exchangeType) {
        // 시크릿 키 복호화하기 -JwtApiUtil
        //쿼리 파라미터에 따라 빗썸 /업비트 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);

        try {//응답
            return apiClient.getBalance(phoneNumber, exchangeType);
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}, error: {}",
                    exchangeType, phoneNumber, e.getMessage(), e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    private ExchangeApiClient getApiClient(ExchangeType exchangeType) {
        return switch (exchangeType) {
            case BITHUMB -> bithumbAdapter;
            case UPBIT -> upbitAdapter;
            case BINANCE -> throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE); // 바이낸스는 후순위
        };
    }
}
