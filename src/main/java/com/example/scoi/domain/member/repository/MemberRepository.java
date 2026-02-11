package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 휴대폰 번호로 회원 조회
    Optional<Member> findByPhoneNumber(String phoneNumber);

    // 휴대폰 번호 중복 체크
    boolean existsByPhoneNumber(String phoneNumber);

    @Modifying
    @Query("UPDATE Member m SET m.loginFailCount = m.loginFailCount + 1 WHERE m.id = :memberId")
    int incrementLoginFailCount(@Param("memberId") Long memberId);

    @Modifying
    @Query("UPDATE Member m SET m.loginFailCount = 0 WHERE m.id = :memberId")
    int resetLoginFailCount(@Param("memberId") Long memberId);
}
