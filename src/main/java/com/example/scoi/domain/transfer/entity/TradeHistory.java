package com.example.scoi.domain.transfer.entity;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.transfer.enums.CoinType;
import com.example.scoi.domain.transfer.enums.TradeType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trade_history")
@EntityListeners(AuditingEntityListener.class)
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType;

    @Column(name = "coin_count", nullable = false)
    private String coinCount;

    @Column(name = "created_at", columnDefinition = "DATETIME(6)")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "trade_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @Column(name = "coin_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CoinType coinType;

    @Column(name = "status")
    private String status;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "idempotent_key", unique = true, nullable = false)
    private String idempotentKey;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;
}
