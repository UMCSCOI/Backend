package com.example.scoi.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Table(name = "member_token")
@EntityListeners(AuditingEntityListener.class)
public class MemberToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;  // RT 최초 발급 시간 (최대 수명 계산용)

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 업데이트 메서드 (issuedAt 유지)
    public void updateToken(String refreshToken, LocalDateTime expirationDate) {
        this.refreshToken = refreshToken;
        this.expirationDate = expirationDate;
    }

    // 업데이트 메서드 (issuedAt 갱신)
    public void updateTokenWithIssuedAt(String refreshToken, LocalDateTime expirationDate, LocalDateTime issuedAt) {
        this.refreshToken = refreshToken;
        this.expirationDate = expirationDate;
        this.issuedAt = issuedAt;
    }
}
