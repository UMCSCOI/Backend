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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try {
            List<BalanceResDTO.BalanceDTO> balances;
            
            switch (exchangeType) {
                case UPBIT:
                    String jwt = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
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
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // API 키를 찾을 수 없는 경우
            if (e instanceof FeignException.NotFound) {
                throw new ChargeException(ChargeErrorCode.EXCHANGE_API_KEY_NOT_FOUND);
            }

            // 나머지 400 에러
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        } catch (FeignException.Unauthorized e) {
            // Error 변환
            ObjectMapper objectMapper = new ObjectMapper();
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 권한 부족
            if (error.error().name().equals("out_of_scope")) {
                throw new ChargeException(ChargeErrorCode.EXCHANGE_FORBIDDEN);
            }

            // 그 이외에는 JWT 관련 오류
            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    // 입금 주소 확인하기
    public List<ChargeResDTO.GetDepositAddress> getDepositAddress(
            String phoneNumber,
            ExchangeType exchangeType,
            List<String> coinType,
            List<String> netType
    ) {

        // 거래소별 요청 보내기
        String token;
        List<ChargeResDTO.GetDepositAddress> result = new ArrayList<>();
        Map<String, String> bindError = new HashMap<>();
        switch (exchangeType){
            case UPBIT:
                for (int idx = 0; idx < coinType.size(); idx++) {
                    try {
                        token = jwtApiUtil
                                .createUpBitJwt(
                                        phoneNumber,
                                        "currency=" + coinType.get(idx) + "&net_type=" + netType.get(idx),
                                        null
                                );

                        UpbitResDTO.GetDepositAddress upbitResult = upbitClient
                                .getDepositAddress(token, coinType.get(idx), netType.get(idx));

                        // 그게 아닌 경우
                        result.add(
                                ChargeConverter.toGetDepositAddress(
                                        upbitResult.currency(),
                                        upbitResult.deposit_address()
                                )
                        );
                        // JWT 토큰 제작 실패
                    } catch (GeneralSecurityException e) {
                        throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                    // 해당 코인의 입금주소가 없는 경우
                    } catch (FeignException.NotFound e) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

                        if (!error.error().name().equals("coin_address_not_found")) {
                            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                        }
                    // 잘못된 파라미터, 거래소에서 지원하지 않는 코인
                    } catch (FeignException.BadRequest e) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

                        // 잘못된 파라미터인 경우: DTO 코인 정보를 잘못 기입
                        switch (error.error().name()) {

                            // 잘못된 파라미터 입력
                            case "validation_error":
                            case "invalid_parameter":
                                throw new ChargeException(ChargeErrorCode.WRONG_COIN_TYPE);

                            // 거래소에서 지원하지 않는 코인
                            case "currency does not have a valid value":
                                bindError.put(coinType.get(idx), netType.get(idx));
                                break;

                            default:
                                throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                        }
                        // JWT 토큰 생성 오류
                    } catch (FeignException.Unauthorized e) {
                        throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                    }
                }
                break;
                case BITHUMB:
                    for (int idx = 0; idx < coinType.size(); idx++){
                        try {
                            token = jwtApiUtil
                                    .createBithumbJwt(
                                            phoneNumber,
                                            "currency="+coinType.get(idx)+"&net_type="+netType.get(idx),
                                            null
                                    );

                            BithumbResDTO.GetDepositAddress bithumbResult = bithumbClient
                                    .getDepositAddress(token, coinType.get(idx), netType.get(idx));

                            // 그게 아닌 경우
                            result.add(
                                    ChargeConverter.toGetDepositAddress(
                                            bithumbResult.currency(),
                                            bithumbResult.deposit_address()
                                    )
                            );
                        // JWT 토큰 제작 실패
                        } catch (GeneralSecurityException e) {
                            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                        // 해당 코인의 입금주소가 없는 경우
                        } catch (FeignException.NotFound e) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

                            if (!error.error().name().equals("coin_address_not_found")) {
                                throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                            }
                        // 잘못된 파라미터, 거래소에서 지원하지 않는 코인
                        } catch (FeignException.BadRequest e) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

                            // 잘못된 파라미터인 경우: DTO 코인 정보를 잘못 기입
                            switch (error.error().name()) {

                                // 잘못된 파라미터 입력
                                case "validation_error":
                                case "invalid_parameter":
                                    throw new ChargeException(ChargeErrorCode.WRONG_COIN_TYPE);

                                // 네트워크 미지원: 넘기기
                                case "request_for_address_of_not_supported_currency":
                                    bindError.put(coinType.get(idx), netType.get(idx));
                                    break;

                                // 거래소에서 지원하지 않는 코인
                                case "currency does not have a valid value":
                                    throw new ChargeException(ChargeErrorCode.NOT_SUPPORT_COIN);

                                default:
                                    throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                            }
                        // JWT 토큰 생성 오류
                        } catch (FeignException.Unauthorized e) {
                            throw new ChargeException(ChargeErrorCode.EXCHANGE_BAD_REQUEST);
                        }
                    }
                    break;
                default:
                    throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
        }

        // 만약 bindError에 값이 있으면
        if (!bindError.isEmpty()){
            throw new ChargeException(ChargeErrorCode.ADDRESS_NOT_FOUND, bindError);
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
