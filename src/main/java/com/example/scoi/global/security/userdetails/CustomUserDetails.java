package com.example.scoi.global.security.userdetails;

import com.example.scoi.domain.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails 구현체
 * Member 엔티티를 래핑하여 인증 정보를 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 현재는 권한 없음, 추후 ROLE 추가 시 확장 가능
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return member.getSimplePassword();
    }

    @Override
    public String getUsername() {
        return member.getPhoneNumber();
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료는 deletedAt으로 체크 (soft delete)
        return member.getDeletedAt() == null;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 5회 이상 로그인 실패 시 계정 잠금
        return member.getLoginFailCount() < 5;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료는 현재 사용하지 않음
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정 활성화 여부 (삭제되지 않은 계정만 활성화)
        return member.getDeletedAt() == null;
    }
}
