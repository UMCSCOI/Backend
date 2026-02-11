package com.example.scoi.domain.member.service;

import com.example.scoi.domain.member.converter.MemberConverter;
import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.domain.member.dto.MemberResDTO;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.entity.MemberFcm;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.repository.MemberApiKeyRepository;
import com.example.scoi.domain.member.repository.MemberFcmRepository;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.global.apiPayload.code.GeneralErrorCode;
import com.example.scoi.global.client.BithumbClient;
import com.example.scoi.global.client.UpbitClient;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.util.HashUtil;
import com.example.scoi.global.util.JwtApiUtil;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.*;

@Slf4j
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
    private final MemberFcmRepository memberFcmRepository;
    private final RedisUtil redisUtil;

    // 인증 완료된 전화번호 접두사
    private static final String VERIFICATION_PREFIX = "verification:";
    // 간편 비밀번호 정규표현식
    private static final String SIMPLE_PASSWORD_REGEX = "^[0-9]{6}$";

    // 내 정보 조회
    public MemberResDTO.MemberInfo getMemberInfo(
            String phoneNumber
    ) {
        // JWT 토큰의 유저 정보 가져오기: 임시
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberConverter.toMemberInfo(member);
    }

    // 간편 비밀번호 변경
    @Transactional
    public Optional<Map<String, String>> changePassword(
            MemberReqDTO.ChangePassword dto,
            String phoneNumber
    ){

        // JWT 토큰 사용자 불러오기
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // DTO로 온 간편 비밀번호 암호화 풀기 (AES)
        String oldSimplePassword;
        String newSimplePassword;

        try {
             oldSimplePassword = new String(hashUtil.decryptAES(dto.oldPassword()));
             newSimplePassword = new String(hashUtil.decryptAES(dto.newPassword()));

            // DTO 비밀번호 포맷 체크
            if (
                    !oldSimplePassword.matches(SIMPLE_PASSWORD_REGEX)
                    || !newSimplePassword.matches(SIMPLE_PASSWORD_REGEX)
            ) {
                throw new IllegalArgumentException();
            }

        } catch (GeneralSecurityException e) {
            Map<String, String> binding = new HashMap<>();
            binding.put("password", "간편 비밀번호 복호화에 실패했습니다.");
            throw new MemberException(GeneralErrorCode.VALIDATION_FAILED, binding);
        } catch (IllegalArgumentException e) {
            Map<String, String> binding = new HashMap<>();
            binding.put("password", "6자리 숫자만 입력 가능합니다.");
            throw new MemberException(GeneralErrorCode.VALIDATION_FAILED, binding);
        }

        // 로그인 횟수가 5 이상인지 확인
        if (member.getLoginFailCount() >= 4){
            throw new MemberException(MemberErrorCode.LOCKED);
        }

        // 기존 비밀번호가 맞는지 확인: 틀렸을 경우 로그인 실패 횟수 증가
        if (!passwordEncoder.matches(oldSimplePassword, member.getSimplePassword())) {
            member.increaseLoginFailCount();
            Map<String, String> binding = new HashMap<>();
            binding.put("loginFailCount", member.getLoginFailCount().toString());
            return Optional.of(binding);
        }

        // 간편 비밀번호 변경: 새 비밀번호 DB 저장 & LoginFailCount = 0
        member.updateSimplePassword(passwordEncoder.encode(newSimplePassword));
        member.resetLoginFailCount();
        return Optional.empty();
    }

    // 간편 비밀번호 재설정
    @Transactional
    public Void resetPassword(
            MemberReqDTO.ResetPassword dto,
            String phoneNumber
    ) {

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 인증된 전화번호인지 확인 (verification:{verificationToken} 키로 조회)
        String tokenKey = VERIFICATION_PREFIX + dto.verificationToken();
        String verifiedPhoneNumber = redisUtil.get(tokenKey);
        if (verifiedPhoneNumber == null || !verifiedPhoneNumber.equals(dto.phoneNumber())) {
            throw new MemberException(MemberErrorCode.UNVERIFIED_PHONE_NUMBER);
        }
        redisUtil.delete(tokenKey);

        // 새 간편 비밀번호 검증
        String newPassword;
        try {
            newPassword = new String(hashUtil.decryptAES(dto.newPassword()));

            // 6자리 숫자가 아닌 경우
            if (!newPassword.matches(SIMPLE_PASSWORD_REGEX)) {
                throw new IllegalArgumentException();
            }
        } catch (GeneralSecurityException e ) {
            Map<String, String> binding = new HashMap<>();
            binding.put("password", "간편 비밀번호 복호화에 실패했습니다.");
            throw new MemberException(GeneralErrorCode.VALIDATION_FAILED, binding);
        } catch (IllegalArgumentException e) {
            Map<String, String> binding = new HashMap<>();
            binding.put("password", "6자리 숫자만 입력 가능합니다.");
            throw new MemberException(GeneralErrorCode.VALIDATION_FAILED, binding);
        }

        // 간편 비밀번호 변경
        member.updateSimplePassword(passwordEncoder.encode(newPassword));

        // 로그인 횟수 -> 0
        member.resetLoginFailCount();
        return null;
    }

    // 거래소 목록 조회
    public List<MemberResDTO.ExchangeList> getExchangeList(
            String phoneNumber
    ){
        // JWT 토큰 사용자 API 키 불러오기
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember_PhoneNumber(phoneNumber);

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
            String phoneNumber
    ) {
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember_PhoneNumber(phoneNumber);

        return memberApiKeyList.stream()
                .map(MemberConverter::toApiKeyList)
                .toList();
    }

    // API키 등록 & 수정
    @Transactional
    public List<String> postPatchApiKey(
            String phoneNumber,
            List<MemberReqDTO.PostPatchApiKey> dto
    ) {

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 기존 등록한 API 리스트 조회
        List<MemberApiKey> memberApiKeyList = memberApiKeyRepository.findAllByMember(member);

        List<String> result = new ArrayList<>();

        // API 리스트 for문
        for (MemberReqDTO.PostPatchApiKey i : dto){

            log.info("[API키 등록]: 시크릿 키: {}", i.secretKey());
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
                log.info("[API키 등록]: 검증 실패");
                log.info(e.getMessage());
                continue;
            }

            log.info("[API키 등록]: 검증 성공");
            // 정상적인 요청
            boolean isExist = false;
            for (MemberApiKey apiKey : memberApiKeyList){

                // 이미 등록된 거래소가 존재할 경우
                if (i.exchangeType().name().equals(apiKey.getExchangeType().name())){
                    isExist = true;
                    apiKey.updateApiKey(i.publicKey(), i.secretKey());
                    result.add(apiKey.getExchangeType().name());
                    log.info("[API키 등록]: API키 변경 완료");
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
                log.info("[API키 등록]: API키 엔티티 생성 완료");
            }
        }
        return result;
    }

    // API키 삭제
    @Transactional
    public Void deleteApiKey(
            String phoneNumber,
            MemberReqDTO.DeleteApiKey dto
    ) {
        // 해당 연동 정보가 없다면
        if (
                !memberApiKeyRepository.existsByMember_PhoneNumberAndExchangeType(
                phoneNumber, dto.exchangeType())
        ){
            throw new MemberException(MemberErrorCode.API_KEY_NOT_FOUND);
        }

        // 있다면 지우기
        memberApiKeyRepository.deleteByMember_PhoneNumberAndExchangeType(phoneNumber, dto.exchangeType());

        return null;
    }

    // FCM 토큰 등록
    @Transactional
    public Void postFcmToken(
            String phoneNumber,
            MemberReqDTO.PostFcmToken dto
    ) {

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // FCM 토큰 저장 (로그인 -> 추가, 디바이스당 추가)
        MemberFcm memberFcm = MemberConverter.toMemberFcm(dto.token(), member);
        memberFcmRepository.save(memberFcm);

        return null;
    }
}
