package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.MemberToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberTokenRepository extends JpaRepository<MemberToken, Long> {

    // 회원 ID로 토큰 조회
    Optional<MemberToken> findByMemberId(Long memberId);

    // 휴대폰 번호로 토큰 조회
    Optional<MemberToken> findByMemberPhoneNumber(String phoneNumber);

    // Refresh Token으로 조회
    Optional<MemberToken> findByRefreshToken(String refreshToken);

    // 회원 ID로 토큰 삭제 (로그아웃 시)
    void deleteByMemberId(Long memberId);

    // 휴대폰 번호로 토큰 삭제
    void deleteByMemberPhoneNumber(String phoneNumber);
}