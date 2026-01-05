package com.example.scoi.domain.member.entity;

import com.example.scoi.domain.member.enums.ExchangeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Table(name = "member_api_key")
public class MemberApiKey {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_type")
    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType;

    @Column(name = "public_key")
    private String publicKey;

    // AesBytesEncryptor로 대칭키 암호화
    @Column(name = "secret_key")
    private String secretKey;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
