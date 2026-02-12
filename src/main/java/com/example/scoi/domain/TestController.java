package com.example.scoi.domain;

import com.example.scoi.domain.websocket.enums.RiseOrFall;
import com.example.scoi.global.util.FcmUtil;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final FcmUtil fcmUtil;

    @GetMapping("/test")
    public String test(){

        // 테스트
        String code = "USDT";
        String current = "10000";
        String percent = "10";
        RiseOrFall riseOrFall = RiseOrFall.RISE;

        // 전체 사용자에게 알림 보내기
        try {
            fcmUtil.sendNotificationForDepegging(
                    code+" 가격 변동 알림",
                    "평소보다 "+code+" 가격이 "+current+"원으로 약 "+percent+"% "+riseOrFall.name()
            );
        } catch (FirebaseMessagingException e){
            log.error("[ FcmUtil ]: 알림 전송 실패, 에러 코드: {}, 스택 트레이스: {}", e.getMessagingErrorCode(), e.getStackTrace());
        }

        return "test";
    }
}
