package com.example.scoi.global.util;

import com.example.scoi.domain.member.repository.MemberFcmRepository;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmUtil {

    private final MemberFcmRepository memberFcmRepository;
    private final FirebaseMessaging firebaseMessaging;

    private static final String DEPEGGING_TOPIC = "Depegging-all";

    /**
     * 디페깅 발생시 유저에게 알림을 전송합니다.
     * @param title 보낼 알림의 제목
     * @param body 보낼 알림의 내용
     */
    @Retryable(
            recover = "sendRecover"
    )
    @Async
    public void sendNotificationForDepegging(
            @NotNull String title,
            @NotNull String body
    ){

        log.info("[ FcmUtil ]: 디페깅 상황 발생, 알림 전송...");

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
                .setTopic(DEPEGGING_TOPIC)
                .build();

        // 알림 전송
        try{
            firebaseMessaging.send(message);
            log.info("[ FcmUtil ]: 알림 전송 성공, 전송 토픽: {}", DEPEGGING_TOPIC);
        } catch (FirebaseMessagingException e){
            log.warn("[ FcmUtil ]: 알림 전송 실패");
        }
    }

    /**
     * 디페깅 알림을 위해 구독합니다.
     * @param fcmTokenList 알림을 구독할 FCM 토큰
     */
    @Retryable(
            recover = "subscriberRecover"
    )
    @Async
    public void subscribeNotificationForDepegging(
            @NotNull List<String> fcmTokenList
    ) throws FirebaseMessagingException {

        log.info("[ FcmUtil ]: 디페깅 알고리즘 구독 중...");
        firebaseMessaging.subscribeToTopic(fcmTokenList, DEPEGGING_TOPIC);
    }

    /**
     * 디페깅 알고리즘 구독을 취소합니다. (로그아웃)
     * @param fcmTokenList 구독 취소할 FCM 토큰
     */
    @Retryable(
            recover = "unsubscriberRecover"
    )
    @Async
    public void unsubscribeNotificationForDepegging(
            @NotNull List<String> fcmTokenList
    ) throws FirebaseMessagingException {

        log.info("[ FcmUtil ]: 디페깅 알고리즘 구독 해제 중...");
        firebaseMessaging.unsubscribeFromTopic(fcmTokenList, DEPEGGING_TOPIC);

    }

    @Recover
    public void sendRecover(
            FirebaseMessagingException e
    ) throws FirebaseException {
        log.warn("[ FcmUtil ]: 디페깅 알고리즘 알림 전송 실패, {}", LocalDateTime.now());
        throw new FirebaseException(e.getErrorCode(), e.getMessage(), e.getCause());
    }

    @Recover
    public void subscriberRecover(
            FirebaseMessagingException e
    ) throws FirebaseException {

        log.warn("[ FcmUtil ]: 디페깅 알고리즘 구독 실패, {}", LocalDateTime.now());
        throw new FirebaseException(e.getErrorCode(), e.getMessage(), e.getCause());
    }

    @Recover
    public void unsubscriberRecover(
            FirebaseMessagingException e
    ) throws FirebaseException {

        log.warn("[ FcmUtil ]: 디페깅 알고리즘 구독 해제 실패, {}", LocalDateTime.now());
        throw new FirebaseException(e.getErrorCode(), e.getMessage(), e.getCause());
    }
}
