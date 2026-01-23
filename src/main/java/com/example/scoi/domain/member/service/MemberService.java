package com.example.scoi.domain.member.service;

import com.example.scoi.domain.member.client.BithumbClient;
import com.example.scoi.domain.member.client.UpbitClient;
import com.example.scoi.domain.member.converter.MemberConverter;
import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.domain.member.dto.MemberResDTO;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.repository.MemberApiKeyRepository;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.global.apiPayload.code.GeneralErrorCode;
import com.example.scoi.global.auth.entity.AuthUser;
import com.example.scoi.global.util.HashUtil;
import com.example.scoi.global.util.JwtApiUtil;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberApiKeyRepository memberApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final HashUtil hashUtil;
    private final JwtApiUtil jwtApiUtil;
    private final BithumbClient bithumbClient;
    private final UpbitClient upbitClient;

    // JwtApiUtil 테스트
    public Void apiTest(
            AuthUser user
    ) throws GeneralSecurityException {
        Member member = memberRepository.findByPhoneNumber(user.getPhoneNumber())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 빗썸
        // 쿼리파라미터 X & Request Body X
        String token = jwtApiUtil.createBithumbJwt(member.getPhoneNumber(),null,null);
        bithumbClient.getAccount(token);

        // 쿼리파라미터 O
        token = jwtApiUtil.createBithumbJwt(member.getPhoneNumber(),"market=KRW-BTC",null);
        bithumbClient.getOrderChance(token,"KRW-BTC");

        // Request Body O
        MemberReqDTO.Test dto = MemberReqDTO.Test.builder().currency("BTC").net_type("BTC").build();
        token = jwtApiUtil.createBithumbJwt(member.getPhoneNumber(), null, dto);
        bithumbClient.getDepositAddress(token, dto);

        // 업비트
        // 쿼리퍼라미터 X & Request Body X
        token = jwtApiUtil.createUpBitJwt(member.getPhoneNumber(), null, null);
        upbitClient.getAccount(token);

        // 쿼리파라미터 O
        token = jwtApiUtil.createUpBitJwt(member.getPhoneNumber(), "market=KRW-BTC", null);
        upbitClient.getOrderChance(token, "KRW-BTC");

        // Request Body O
        dto = MemberReqDTO.Test.builder().currency("BTC").net_type("BTC").build();
        token = jwtApiUtil.createUpBitJwt(member.getPhoneNumber(), null, dto);
        upbitClient.getDepositAddress(token, dto);

        return null;
    }

    // 내 정보 조회
    public MemberResDTO.MemberInfo getMemberInfo(
            AuthUser user
    ) {
        // JWT 토큰의 유저 정보 가져오기: 임시
        Member member = memberRepository.findByPhoneNumber(user.getPhoneNumber())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberConverter.toMemberInfo(member);
    }

    // 휴대폰 번호 변경
    public MemberResDTO.ChangePhone changePhone(
            MemberReqDTO.ChangePhone dto,
            AuthUser user
    ) {
        // SMS 인증번호 검증을 어떻게 처리할지 물어보기!
        return null;
    }

    // 간편 비밀번호 변경
    @Transactional
    public Optional<Map<String, String>> changePassword(
            MemberReqDTO.ChangePassword dto,
            AuthUser user
    ) throws GeneralSecurityException {

        // JWT 토큰 사용자 불러오기
        Member member = memberRepository.findByPhoneNumber(user.getPhoneNumber())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // DTO로 온 간편 비밀번호 암호화 풀기 (AES)
        byte[] oldSimplePassword = hashUtil.decryptAES(dto.oldPassword());
        byte[] newSimplePassword = hashUtil.decryptAES(dto.newPassword());

        // 로그인 횟수가 5 이상인지 확인
        if (member.getLoginFailCount() >= 4){
            throw new MemberException(MemberErrorCode.LOCKED);
        }

        // DTO 비밀번호 포맷 체크
        String regex = "^[0-9]{6}$";
        if (!new String(oldSimplePassword).matches(regex) || !new String(newSimplePassword).matches(regex)) {
            Map<String, String> binding = new HashMap<>();
            binding.put("password", "6자리 숫자만 입력 가능합니다.");
            throw new MemberException(GeneralErrorCode.VALIDATION_FAILED, binding);
        }

        // 기존 비밀번호가 맞는지 확인: 틀렸을 경우 로그인 실패 횟수 증가
        if (!passwordEncoder.matches(new String(oldSimplePassword), member.getSimplePassword())) {
            member.increaseLoginFailCount();
            Map<String, String> binding = new HashMap<>();
            binding.put("loginFailCount", member.getLoginFailCount().toString());
            return Optional.of(binding);
        }

        // 간편 비밀번호 변경: 새 비밀번호 DB 저장 & LoginFailCount = 0
        member.updateSimplePassword(passwordEncoder.encode(new String(newSimplePassword)));
        member.resetLoginFailCount();
        return Optional.empty();
    }

    // 간편 비밀번호 재설정
    public Void resetPassword(
            MemberReqDTO.ResetPassword dto,
            AuthUser user
    ) {
        // SMS 인증 방식에 따라 결정!
        return null;
    }

    // 거래소 목록 조회
    public List<MemberResDTO.ExchangeList> getExchangeList(
            AuthUser user
    ){
        // JWT 토큰 사용자 API 키 불러오기
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember_PhoneNumber(user.getPhoneNumber());

        List<MemberResDTO.ExchangeList> result = new ArrayList<>();
        for (ExchangeType exchangeType : ExchangeType.values()) {
            if (memberApiKeyList.stream()
                    .anyMatch(apiKey -> apiKey.getExchangeType().equals(exchangeType))
            ) {
                result.add(MemberConverter.toExchangeList(exchangeType.toString(), true));
            } else {
                result.add(MemberConverter.toExchangeList(exchangeType.toString(), false));
            }
        }

        return result;
    }

    // API키 목록 조회
    public List<MemberResDTO.ApiKeyList> getApiKeyList(
            AuthUser user
    ) {
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember_PhoneNumber(user.getPhoneNumber());

        return memberApiKeyList.stream()
                .map(MemberConverter::toApiKeyList)
                .toList();
    }

    // API키 등록 & 수정
    public List<String> postPatchApiKey(
            AuthUser user,
            List<MemberReqDTO.PostPatchApiKey> dto
    ) {

        Member member = memberRepository.findByPhoneNumber(user.getPhoneNumber())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 기존 등록한 API 리스트 조회
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember(member);

        List<String> result = new ArrayList<>();

        // API 리스트 for문
        for (MemberReqDTO.PostPatchApiKey i : dto){

            // 실제 API키가 맞는지 검증: JWT 토큰 생성한 뒤 실제 요청을 보냈을때 200 오면 OK
            try {
                String token = jwtApiUtil.createJwtWithApiKeys(i.publicKey(),i.secretKey(),i.exchangeType());
                switch (i.exchangeType().name()){
                    case "BITHUMB":
                        bithumbClient.getAccount(token);
                        break;
                    case "UPBIT":
                        upbitClient.getAccount(token);
                        break;
                }
            // JWT 토큰 생성 실패시: dto 거래소 타입이 잘못됨, 잘못된 API 키
            // 요청을 보낼때 에러 (4XX) 발생시
            } catch (GeneralSecurityException | IllegalArgumentException | FeignException e) {
                continue;
            }

            // 정상적인 요청
            boolean isExist = false;
            for (MemberApiKey apiKey : memberApiKeyList){

                // 이미 등록된 거래소가 존재할 경우
                if (i.exchangeType().name().equals(apiKey.getExchangeType().name())){
                    isExist = true;
                    apiKey.updateApiKey(i.publicKey(), i.secretKey());
                    result.add(apiKey.getExchangeType().name());
                }
            }

            // 등록되지 않은 거래소인 경우
            // 추가
            if (!isExist){
                MemberApiKey apiKey = MemberConverter.toMemberApiKey(
                        i.exchangeType(),
                        i.publicKey(),
                        i.secretKey(),
                        member
                );
                memberApiKeyRepository.save(apiKey);
                result.add(i.exchangeType().name());
            }
        }
        return result;
    }

    // API키 삭제
    @Transactional
    public Void deleteApiKey(
            AuthUser user,
            MemberReqDTO.DeleteApiKey dto
    ) {
        // 해당 연동 정보가 없다면
        if (!memberApiKeyRepository.existsByMember_phoneNumberAndExchangeType(
                user.getPhoneNumber(), dto.exchangeType())
        ){
            throw new MemberException(MemberErrorCode.API_KEY_NOT_FOUND);
        }

        // 있다면 지우기
        memberApiKeyRepository.deleteByMember_PhoneNumberAndExchangeType(user.getPhoneNumber(), dto.exchangeType());

        return null;
    }
}
