package com.example.scoi.domain.member.entity;

import com.example.scoi.domain.member.enums.MemberType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
@SQLDelete(sql = "UPDATE member SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "english_name")
    private String englishName;

    @Column(name = "korean_name")
    private String koreanName;

    @Column(name = "resident_number")
    private String residentNumber;

    // BCrypt 단방향 암호화
    @Column(name = "simple_password")
    private String simplePassword;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "member_type")
    @Enumerated(EnumType.STRING)
    private MemberType memberType;

    @Column(name = "is_bio_registered")
    private Boolean isBioRegistered;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "login_fail_count")
    private Integer loginFailCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "profile_image_url")
    private String profileImageUrl;
}
