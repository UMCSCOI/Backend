package com.example.scoi.domain.member.repository;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.enums.ExchangeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberApiKeyRepository extends JpaRepository<MemberApiKey,Long> {
   
    Optional<MemberApiKey> findByMember_PhoneNumberAndExchangeType(
            @NotNull String phoneNumber, 
            @NotNull ExchangeType exchangeType
    );

    List<MemberApiKey> findAllByMember_PhoneNumber(String memberPhoneNumber);

    List<MemberApiKey> findAllByMember(Member member);

    void deleteByMember_PhoneNumberAndExchangeType(String memberPhoneNumber, ExchangeType exchangeType);

    boolean existsByMember_PhoneNumberAndExchangeType(String memberPhoneNumber, ExchangeType exchangeType);
}
