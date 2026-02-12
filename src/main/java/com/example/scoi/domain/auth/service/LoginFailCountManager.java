package com.example.scoi.domain.auth.service;

import com.example.scoi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginFailCountManager {

    private final MemberRepository memberRepository;

    /**
     * 로그인 실패 카운트를 별도 트랜잭션에서 증가시킵니다.
     * REQUIRES_NEW로 외부 트랜잭션 롤백과 무관하게 커밋됩니다.
     * @Modifying @Query로 L1 캐시 무관하게 DB 직접 업데이트합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increaseFailCount(Long memberId) {
        memberRepository.incrementLoginFailCount(memberId);
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new IllegalStateException("Member not found: " + memberId))
            .getLoginFailCount();
    }

    /**
     * 로그인 실패 카운트를 별도 트랜잭션에서 초기화합니다.
     * SMS 재인증으로 계정 잠금 해제 시 사용 — 이후 비밀번호가 틀려도 잠금 해제는 유지됩니다.
     * @Modifying @Query로 L1 캐시 무관하게 DB 직접 업데이트합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailCount(Long memberId) {
        memberRepository.resetLoginFailCount(memberId);
    }
}
