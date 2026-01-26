package com.example.scoi.global.util;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberFcm;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.repository.MemberFcmRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FcmUtil {

    private final MemberFcmRepository memberFcmRepository;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * 특정 유저에게 알림을 전송합니다.
     * @param title 보낼 알림의 제목
     * @param body 보낼 알림의 내용
     * @param member 특정 유저
     * @return null
     * @throws FirebaseMessagingException 실패시 발생
     */
    public Void sendNotification(
            @NotNull String  title,
            @NotNull String body,
            @NotNull Member member
    ) throws FirebaseMessagingException {
        // 유저 FCM 토큰 찾기
        MemberFcm memberFcm = memberFcmRepository.findByMember(member)
                .orElseThrow(() -> new MemberException(MemberErrorCode.FCM_TOKEN_NOT_FOUND));

        // 안드로이드 설정
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        // 보낼 알림 구성
        Message message = Message.builder()
                // payLoad는 Data만
                .putData("title", title)
                .putData("body", body)
                // 우선순위는 high
                .setAndroidConfig(androidConfig)
                .setToken(memberFcm.getFcmToken())
                .build();

        // 알림 전송: 실패시
        firebaseMessaging.send(message);

        return null;
    }
}
