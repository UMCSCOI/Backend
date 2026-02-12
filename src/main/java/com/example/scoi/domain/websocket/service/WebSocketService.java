package com.example.scoi.domain.websocket.service;

import com.example.scoi.domain.websocket.dto.UpbitResDTO;
import com.example.scoi.domain.websocket.enums.RiseOrFall;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.util.FcmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final FcmUtil fcmUtil;
    private final RedisUtil redisUtil;

    // 웹소켓 관련된 작업 Redis 접두사
    private static final String WEBSOCKET_PREFIX = "websocket:";
    private static final String BASELINE = "baseline:";
    private static final String PRICE = "price";
    private static final String TICK = "tick";
    private static final String COOLDOWN = "cooldown";
    private static final String DURATION = "duration";

    private static final Float A = 0.01F;               // a
    private static final Float DEV_TH = 0.06F;          // 6%: 가격 변화 민감도
    private static final Integer DUR_TH_SEC = 600;      // 10분: 가격 급등, 급락 시 알림 전송 기준
    private static final Long COOLDOWN_SEC = 3600L;   // 1시간: 재알림 주기
    private static final Double EPS = 1e9;

    // 실시간 가격 변동 체크
    public void ticker(UpbitResDTO.Ticker dto) {

        String code = dto.cd().replace("KRW-","");

        // Redis에서 baseline, prevTs, duration 가져오기
        // BigDecimal로 부동 소수점 오차 해결
        Double baseline = (redisUtil.exists(WEBSOCKET_PREFIX+code+BASELINE+PRICE))?
                Double.valueOf(redisUtil.get(WEBSOCKET_PREFIX+code+BASELINE+PRICE)):dto.tp();

        BigDecimal prevTs = (redisUtil.exists(WEBSOCKET_PREFIX+code+BASELINE+TICK))?
                new BigDecimal(redisUtil.get(WEBSOCKET_PREFIX+code+BASELINE+TICK)):new BigDecimal(dto.ttms());

        Double duration = (redisUtil.exists(WEBSOCKET_PREFIX+code+DURATION))?
                Double.valueOf(redisUtil.get(WEBSOCKET_PREFIX+code+DURATION)):0.0;

        BigDecimal nowTs = new BigDecimal(dto.ttms());

        // deltaTs 계산
        BigDecimal deltaSec = (nowTs.subtract(prevTs))
                .divide(new BigDecimal(1000), 10, RoundingMode.HALF_UP);

        if (deltaSec.compareTo(BigDecimal.ZERO) < 0) deltaSec = BigDecimal.ZERO;
        if (deltaSec.compareTo(new BigDecimal(180)) > 0) deltaSec = new BigDecimal(180);

        // 휴식 시간이 지났는가?: 있다면 진행 X
        if (!redisUtil.exists(WEBSOCKET_PREFIX+code+BASELINE+COOLDOWN)
        ) {
            BigDecimal devNumerator = new BigDecimal(Math.abs(dto.tp() - baseline));
            BigDecimal dev = devNumerator.divide(BigDecimal.valueOf(Math.max(baseline, EPS)), RoundingMode.HALF_UP);

            log.info("coin: {}, dev: {}, baseline: {}, tp: {}", code, dev, baseline, dto.tp());

            // duration 누적/리셋
            if (dev.compareTo(BigDecimal.valueOf(DEV_TH)) >= 0){
                duration += deltaSec.doubleValue();
            } else {
                duration = 0.0;
            }

            redisUtil.set(WEBSOCKET_PREFIX+code+DURATION, String.valueOf(duration));

            // 알림 조건
            if (duration >= DUR_TH_SEC){

                // 기준치와 현재가 퍼센트 계산
                String percent = String.format("%.2f", (dto.tp()-baseline)/baseline*100);
                RiseOrFall riseOrFall = (percent.compareTo("0.00") >= 0)? RiseOrFall.RISE : RiseOrFall.FALL;

                // 전체 사용자에게 알림 보내기
                fcmUtil.sendNotificationForDepegging(
                        code+" 가격 변동 알림",
                        "평소보다 "+code+" 가격이 "+dto.tp()+"원으로 약 "+percent+"% "+riseOrFall.name()
                );

                // 쿨타임 저장: TTL 쿨타임 저장 시간 (1시간)
                redisUtil.set(
                        WEBSOCKET_PREFIX+code+BASELINE+COOLDOWN,
                        String.valueOf(COOLDOWN_SEC),
                        COOLDOWN_SEC,
                        TimeUnit.SECONDS
                );
            }
        }

        // baseline 업데이트
        redisUtil.set(WEBSOCKET_PREFIX+code+BASELINE+PRICE, String.valueOf(A*dto.tp() + (1-A)*baseline));
        redisUtil.set(WEBSOCKET_PREFIX+code+BASELINE+TICK, String.valueOf(dto.ttms()));
    }
}
