package com.example.scoi.domain.member.entity;

import com.example.scoi.domain.member.enums.MemberType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
@SQLDelete(sql = "UPDATE member SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "english_name", length = 50, nullable = false)
    private String englishName;

    @Column(name = "korean_name", length = 5, nullable = false)
    private String koreanName;

    @Column(name = "resident_number", nullable = false)
    private String residentNumber;

    // BCrypt 단방향 암호화
    @Column(name = "simple_password", nullable = false)
    private String simplePassword;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    // 기본값 = 개인 회원
    @Column(name = "member_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberType memberType = MemberType.INDIVIDUAL;

    @Column(name = "is_bio_registered", nullable = false)
    @Builder.Default
    private Boolean isBioRegistered = false;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // 업데이트
    public void increaseLoginFailCount(){
        this.loginFailCount++;
    }

    public void resetLoginFailCount(){
        this.loginFailCount = 0;
    }

    public void updateSimplePassword(String simplePassword){ this.simplePassword = simplePassword; }
}
