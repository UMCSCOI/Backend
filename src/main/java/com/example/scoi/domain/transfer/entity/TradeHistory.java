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

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(name = "target_wallet")
    private String targetWallet;

    @Column(name = "is_favorite")
    private Boolean isFavorite;

    @Column(name = "exchange_type")
    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType;

    @Column(name = "coin_count")
    private Long coinCount;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "total_balance")
    private Long totalBalance;

    @Column(name = "trade_type")
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @Column(name = "coin_type")
    @Enumerated(EnumType.STRING)
    private CoinType coinType;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
