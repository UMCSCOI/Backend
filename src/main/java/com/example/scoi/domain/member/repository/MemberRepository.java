package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
    Optional<Member> findByPhoneNumber(String phoneNumber);
}
