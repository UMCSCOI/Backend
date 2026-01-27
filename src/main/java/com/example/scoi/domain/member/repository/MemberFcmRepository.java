package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberFcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberFcmRepository extends JpaRepository<MemberFcm, Long>{
    Optional<MemberFcm> findByMember(Member member);
}
