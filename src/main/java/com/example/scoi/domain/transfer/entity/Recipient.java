package com.example.scoi.domain.transfer.entity;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.MemberType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipient")
public class Recipient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress; // 출금 주소

    // 수취인 기본 정보
    @Column(name = "recipient_en_name", length = 50, nullable = false)
    private String recipientEnName;
    @Column(name = "recipient_ko_name", length = 5, nullable = false)
    private String recipientKoName;

    @Column(name = "recipient_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberType recipientType;

    // 수취인 법인 영문명 (개인일 경우 null)
    @Column(name = "recipient_corp_en_name", length = 50, nullable = true)
    private String recipientCorpEnName;
    // 수취인 법인 국문명 (개인일 경우 null)
    @Column(name = "recipient_corp_ko_name", length = 50, nullable = true)
    private String recipientCorpKoName;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void changeToFavoriteFalse(){
        isFavorite = false;
    }

    public void changeToFavoriteTrue(){
        isFavorite = true;
    }
}
