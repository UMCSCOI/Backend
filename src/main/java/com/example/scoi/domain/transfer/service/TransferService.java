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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferService {
    private final TradeHistoryRepository tradeHistoryRepository;
    private final MemberRepository memberRepository;

    // 최근 수취인 조회 로직
    public TransferResDTO.RecipientListDTO getRecentRecipients(
            Member member, String cursor, int limit
    ) {
        // 테스트 용
        if (member == null) {
            member = memberRepository.findById(1L) // DB에 미리 생성해둔 1번 유저 사용
                    .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
        }

        // 1. 커서 디코딩 (Base64 형태의 커서에서 시간과 ID 추출)
        LocalDateTime lastTime = null;
        Long lastId = null;
        if (cursor != null && !cursor.isEmpty()) {
            TransferCursorUtils.CursorContents contents = TransferCursorUtils.decode(cursor);
            lastTime = contents.getTimestamp();
            lastId = contents.getId();
        }

        // 2. DB 조회 (limit + 1을 가져와서 다음 페이지 여부를 확인)
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<TradeHistory> histories = tradeHistoryRepository.findRecentUniqueRecipients(
                member.getId(), lastTime, lastId, pageRequest);

        // 3. 페이징 데이터 처리
        boolean hasNext = histories.size() > limit;
        List<TradeHistory> resultItems = hasNext ? histories.subList(0, limit) : histories;

        // 4. 다음 커서 생성
        String nextCursor = (hasNext && !resultItems.isEmpty())
                ? TransferCursorUtils.encode(resultItems.get(resultItems.size() - 1).getCreatedAt(),
                resultItems.get(resultItems.size() - 1).getId())
                : null;

        // DTO로 변환 및 반환
        return TransferConverter.toRecentRecipientListDTO(resultItems, nextCursor, hasNext);
    }
}
