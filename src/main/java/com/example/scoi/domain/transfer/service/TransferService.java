package com.example.scoi.domain.transfer.service;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.domain.transfer.converter.TransferConverter;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.TradeHistory;
import com.example.scoi.domain.transfer.repository.TradeHistoryRepository;
import com.example.scoi.domain.transfer.utils.TransferCursorUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferService {
    private final TradeHistoryRepository tradeHistoryRepository;
    private final MemberRepository memberRepository;
    private final RecipientRepository recipientRepository;

    // 최근 수취인 조회 메서드
    public TransferResDTO.RecipientListDTO findRecentRecipients(Member member, String cursor, int limit){
        return getRecipients(member, cursor, false, limit);
    }

    // 즐겨찾기 수취인 조회 메서드
    public TransferResDTO.RecipientListDTO findFavoriteRecipients(Member member, String cursor, int limit){

        return getRecipients(member, cursor, true, limit);
    }

    // 수취인 조회 로직
    public TransferResDTO.RecipientListDTO getRecipients(
            Member member, String cursor, boolean isFavorite, int limit
    ) {
        // 테스트 용
        if (member == null) {
            member = memberRepository.findById(1L) // DB에 미리 생성해둔 1번 유저 사용
                    .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
        }

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
    public TransferResDTO.RecipientListDTO findFavoriteRecipients(Member member, String cursor, int limit){
        // 테스트 용
        if (member == null) {
            member = memberRepository.findById(1L) // DB에 미리 생성해둔 1번 유저 사용
                    .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
        }

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
}
