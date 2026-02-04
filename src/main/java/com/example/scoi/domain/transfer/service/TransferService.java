package com.example.scoi.domain.transfer.service;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.auth.exception.AuthException;
import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.domain.transfer.converter.TransferConverter;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.Recipient;
import com.example.scoi.domain.transfer.entity.TradeHistory;
import com.example.scoi.domain.transfer.exception.TransferException;
import com.example.scoi.domain.transfer.exception.code.TransferErrorCode;
import com.example.scoi.domain.transfer.repository.RecipientRepository;
import com.example.scoi.domain.transfer.repository.TradeHistoryRepository;
import com.example.scoi.domain.transfer.utils.TransferCursorUtils;
import com.example.scoi.domain.transfer.utils.WalletUtils;
import com.example.scoi.global.client.BithumbClient;
import com.example.scoi.global.client.UpbitClient;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.client.dto.ClientErrorDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import com.example.scoi.global.util.JwtApiUtil;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class TransferService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final MemberRepository memberRepository;
    private final RecipientRepository recipientRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtApiUtil jwtApiUtil;
    private final BithumbClient bithumbClient;
    private final UpbitClient upbitClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 최근 수취인 조회 메서드
    public TransferResDTO.RecipientListDTO findRecentRecipients(String phoneNumber, String cursor, int limit) {

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 커서 디코딩 (Base64 형태의 커서에서 시간과 historyID 추출)
        LocalDateTime lastTime = null;
        Long lastId = null;
        if (cursor != null && !cursor.isEmpty()) {
            TransferCursorUtils.CursorContents contents = TransferCursorUtils.decode(cursor);
            lastTime = contents.getTimestamp(); // 마지막 조회 시간
            lastId = contents.getId(); // 마지막 거래 내역
        }

        // DB 조회
        PageRequest pageRequest = PageRequest.of(0, limit);
        Slice<TradeHistory> histories = tradeHistoryRepository.findRecentUniqueRecipients(
                member.getId(), lastTime, lastId, pageRequest);

        // 수취인 목록 3개와 다음이 있는지 확인
        List<TradeHistory> content = histories.getContent();
        boolean hasNext = histories.hasNext();

        // 다음 커서 인코딩(없다면 null)
        String nextCursor = (hasNext && !content.isEmpty())
                ? TransferCursorUtils.encode( content.getLast().getCreatedAt(),
                content.getLast().getId() )
                : null;

        // DTO로 변환 및 반환
        return TransferConverter.toRecentRecipientListDTO(content, nextCursor, hasNext);
    }

    // 즐겨찾기 수취인 조회 메서드
    public TransferResDTO.RecipientListDTO findFavoriteRecipients(String phoneNumber, String cursor, int limit){

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 커서 디코딩 (Base64 형태의 RecipientID 추출)
        Long lastId = null;
        if (cursor != null && !cursor.isEmpty()) {
            TransferCursorUtils.CursorContents contents = TransferCursorUtils.decode(cursor);
            lastId = contents.getId(); // 마지막 거래 내역
        }

        // DB 조회
        PageRequest pageRequest = PageRequest.of(0, limit);
        Slice<Recipient> recipients = recipientRepository.findByMemberIdAndIsFavoriteTrue(member.getId(), lastId, pageRequest);

        // 수취인 목록 3개와 다음이 있는지 확인
        List<Recipient> content = recipients.getContent();
        boolean hasNext = recipients.hasNext();

        // 다음 커서 인코딩 (없다면 null)
        String nextCursor = (hasNext && !content.isEmpty())
                ? TransferCursorUtils.encode( content.getLast().getId() )
                : null;

        // DTO로 변환 및 반환
        return TransferConverter.toFavoriteRecipientListDTO(content, nextCursor, hasNext);
    }

    // 즐겨찾기 등록 메서드
    @Transactional
    public Long addFavoriteRecipient(String phoneNumber, TransferReqDTO.RecipientInformation recipientInformation) {

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 이미 즐겨찾기로 등록된 사용자는 그대로 반환
        if(recipientRepository.existsByMemberIdAndWalletAddress(member.getId(), recipientInformation.walletAddress())){
            throw new TransferException(TransferErrorCode.EXIST_FAVORITE_RECIPIENT);
        }

        // 수취인 정보가 옳바른지 검증
        validateRecipient(recipientInformation);

        Recipient recipient = TransferConverter.toFavoriteRecipient(recipientInformation, member);
        Recipient saved = recipientRepository.save(recipient);

        return saved.getId();
    }


    // 즐겨찾기 수취인으로 변경
    public Long changeToFavoriteRecipient(Long recipientId) {

        // id로 수취인 조회
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new TransferException(TransferErrorCode.MEMBER_NOT_FOUND));

        // 이미 즐겨찾기로 등록된 사용자
        if(recipient.getIsFavorite() == true){
            return recipientId;
        }

        // 해당하는 수취인의 좋아요 정보를 true로 변경
        recipient.changeToFavoriteTrue();

        Recipient changed = recipientRepository.save(recipient);

        return changed.getId();
    }


    // 즐겨찾기 수취인 등록 해제
    public Long changeToNotFavoriteRecipient(Long recipientId) {

        // id로 수취인 조회
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new TransferException(TransferErrorCode.MEMBER_NOT_FOUND));

        // 이미 즐겨찾기가 해제된 사용자
        if(recipient.getIsFavorite() == false){
            return recipientId;
        }

        // 해당하는 수취인의 좋아요 정보를 false로 변경
        recipient.changeToFavoriteFalse();

        Recipient changed = recipientRepository.save(recipient);

        return changed.getId();
    }

    public TransferResDTO.CheckRecipientResDTO checkRecipientInput(
            TransferReqDTO.RecipientInformation recipientInformation,
            String phoneNumber
    ) {

        // 1. 수취인 입력 값 자체 검증 (거래소 자체에서 검증하는 API 존재 X)
        validateRecipient(recipientInformation);

        // 2. 출금 가능 금액 조회
        String token;
        String currency = String.valueOf(recipientInformation.coinType());
        String netType = recipientInformation.netType();

        try{
            switch (recipientInformation.exchangeType()){
                case UPBIT :
                    token = jwtApiUtil.createUpBitJwt(
                            phoneNumber,
                            "currency="+currency + "&" + "net_type="+netType,
                            null);

                    UpbitResDTO.WithdrawsChance upbitResult = upbitClient.getWithdrawsChance(token, currency, netType);

                    return TransferConverter.toCheckRecipientResDTO(recipientInformation, upbitResult);

                case BITHUMB:
                    token = jwtApiUtil.createBithumbJwt(
                            phoneNumber,
                            "currency="+currency + "&" + "net_type="+netType,
                            null);

                    BithumbResDTO.WithdrawsChance bithumResult = bithumbClient.getWithdrawsChance(token, currency, netType);

                    return TransferConverter.toCheckRecipientResDTO(recipientInformation, bithumResult);

                default:
                    throw new TransferException(TransferErrorCode.UNSUPPORTED_EXCHANGE);
            }

        // 토큰을 못 만들었을 경우
        }catch (GeneralSecurityException e){
            throw new RuntimeException(e);

        // 잘못된 요청 형식
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            String rawBody = e.contentUTF8(); // 원본 응답 저장
            log.error(">>>> 거래소 응답 원본: {}", rawBody); // 에러 로그 원본

            ClientErrorDTO.Errors error = null;
            error = objectMapper.readValue(rawBody, ClientErrorDTO.Errors.class);
            log.error(">>>> 거래소 에러 코드명: {}", error.error().name()); // 파싱된 거래소 에러 코드명 확인

            // 파라미터(네트워크 타입)가 잘못된 경우
            if (error.error().name().equals("validation_error")) {
                throw new TransferException(TransferErrorCode.INVALID_NETWORK_TYPE);
            }

            // 나머지 400 에러
            throw new TransferException(TransferErrorCode.EXCHANGE_BAD_REQUEST);

        // 권한이 부족한 경우
        } catch (FeignException.Unauthorized e) {
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 권한이 부족한 경우
            if (error.error().name().equals("out_of_scope")) {
                throw new TransferException(TransferErrorCode.EXCHANGE_FORBIDDEN);
            }

            // 나머지 JWT 관련 오류
            throw new TransferException(TransferErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    public TransferResDTO.QuoteValidDTO checkQuotes(TransferReqDTO.Quote quotes) {

        // 1. 이체 금액 + 수수료 계산
        BigDecimal amount = new BigDecimal(quotes.amount());
        BigDecimal fee = new BigDecimal(quotes.networkFee());

        BigDecimal total = amount.add(fee);

        // 2. 잔고에 해당하는 금액이 존재하는지 확인
        BigDecimal available = new BigDecimal(quotes.available());

        if(available.compareTo(total) < 0){
            throw new TransferException(TransferErrorCode.INSUFFICIENT_BALANCE);
        }

        // 3. 응답 변환 및 반환
        return TransferConverter.toQuoteValidDTO(
                amount.toPlainString(),
                fee.toPlainString(),
                total.toPlainString()
        );
    }

    // 이체
    public TransferResDTO.WithdrawResult executeWithdraw(String phoneNumber, TransferReqDTO.WithdrawRequest request) {

        // 1. 간편 비밀번호 검증
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getLoginFailCount() >= 5) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.simplePassword(), member.getSimplePassword())) {
            member.increaseLoginFailCount();
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }
        // 비밀번호 일치 시 실패 횟수 초기화
        member.resetLoginFailCount();

        // 2. 이체하기
        String token;

        try {
            switch (request.exchangeType()) {
                case UPBIT:
                    TransferReqDTO.UpbitWithdrawRequest upbitDTO = TransferConverter.toUpbitWithdrawRequest(request);
                    token = jwtApiUtil.createUpBitJwt(phoneNumber, null, upbitDTO);

                    UpbitResDTO.WithdrawResDTO upbitRes = upbitClient.withdrawCoin(token, upbitDTO);
                    TransferResDTO.WithdrawResult upbitResult = TransferConverter.toWithdrawResult(upbitRes);

                    return upbitResult;

                case BITHUMB:
                    TransferReqDTO.BithumbWithdrawRequest bithumbDTO = TransferConverter.toBithumbWithdrawRequest(request);
                    token = jwtApiUtil.createBithumbJwt(phoneNumber, null, bithumbDTO);

                    BithumbResDTO.WithdrawResDTO bithumRes = bithumbClient.withdrawCoin(token, bithumbDTO);
                    TransferResDTO.WithdrawResult bithumbResult = TransferConverter.toWithdrawResult(bithumRes);

                    return bithumbResult;

                default:
                    throw new TransferException(TransferErrorCode.UNSUPPORTED_EXCHANGE);
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        catch (FeignException.BadRequest | FeignException.NotFound e) {
            String rawBody = e.contentUTF8(); // 원본 응답 저장
            log.error(">>>> 거래소 응답 원본: {}", rawBody); // 에러 로그 원본

            ClientErrorDTO.Errors error = null;
            error = objectMapper.readValue(rawBody, ClientErrorDTO.Errors.class);
            log.error(">>>> 거래소 에러 코드명: {}", error.error().name()); // 파싱된 거래소 에러 코드명 확인

            // 파라미터가 잘못된 경우
            if (error.error().name().equals("validation_error")) {
                throw new TransferException(TransferErrorCode.INVALID_INPUT);
            }

            // 네트워크가 잘못된 경우
            if (error.error().name().equals("invalid_network_type")) {
                throw new TransferException(TransferErrorCode.INVALID_NETWORK_TYPE);
            }

            // 지갑 주소가 올바르지 않은 경우
            if (error.error().name().equals("invalid_withdraw_address")) {
                throw new TransferException(TransferErrorCode.INVALID_WALLET_ADDRESS);
            }

            // 거래소에서 요청을 처리하지 못한 경우
            if(error.error().name().equals("request_fail")) {
                throw new TransferException(TransferErrorCode.EXCHANGE_BAD_REQUEST);
            }

            // 등록된 출금 주소가 아닌 경우
            if(error.error().name().equals("withdraw_address_not_registered")) {
                throw new TransferException(TransferErrorCode.UNREGISTERED_WALLET_ADDRESS);
            }

            // 출금 시스템 점검 중인 경우
            if(error.error().name().equals("withdraw_maintain")) {
                throw new TransferException(TransferErrorCode.TRANSFER_CHECK);
            }

            // 나머지 400 에러
            throw new TransferException(TransferErrorCode.EXCHANGE_BAD_REQUEST);

            // 권한이 부족한 경우
        } catch (FeignException.Unauthorized e) {
            ClientErrorDTO.Errors error = objectMapper.readValue(e.contentUTF8(), ClientErrorDTO.Errors.class);

            // 권한이 부족한 경우
            if (error.error().name().equals("out_of_scope")) {
                throw new TransferException(TransferErrorCode.EXCHANGE_FORBIDDEN);
            }

            // 나머지 JWT 관련 오류
            throw new TransferException(TransferErrorCode.EXCHANGE_BAD_REQUEST);
        }
    }

    // 수취인 입력값 검증 메서드
    private void validateRecipient(TransferReqDTO.RecipientInformation recipient) {
        // 1. 지갑 주소와 수취인 정보가 비어있는 경우
        if (recipient.walletAddress().isEmpty() || recipient.recipientName().isEmpty()) {
            throw new TransferException(TransferErrorCode.INVALID_RECIPIENT_INFORMATION);
        }
        // 2. 지갑 주소 형식이 올바르지 않은 경우
        if (!WalletUtils.isValidAddress(recipient.walletAddress())) {
            throw new TransferException(TransferErrorCode.INVALID_WALLET_ADDRESS);
        }
        // 3. 법인 회원인데 법인 정보가 없는 경우
        if(recipient.memberType() == MemberType.CORPORATION &&
                (recipient.corpKoreanName().isBlank() || recipient.corpEnglishName().isBlank())
        ){
            throw new TransferException(TransferErrorCode.INVALID_RECIPIENT_INFORMATION);
        }
        // 4. 수취인 이름이 2 - 5자가 아닌 경우
        if(recipient.recipientName().length() > 5 || recipient.recipientName().length() < 2){
            throw new TransferException(TransferErrorCode.INVALID_RECIPIENT_INFORMATION);
        }
    }
}
