package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.enums.ExchangeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberApiKeyRepository extends JpaRepository<MemberApiKey, Long> {
    
    /**
     * phoneNumber와 exchangeType으로 MemberApiKey 조회
     * Member와 조인하여 phoneNumber로 조회
     */
    @Query("SELECT mak FROM MemberApiKey mak " +
           "JOIN mak.member m " +
           "WHERE m.phoneNumber = :phoneNumber AND mak.exchangeType = :exchangeType")
    Optional<MemberApiKey> findByMemberPhoneNumberAndExchangeType(
            @Param("phoneNumber") String phoneNumber,
            @Param("exchangeType") ExchangeType exchangeType
    );
}