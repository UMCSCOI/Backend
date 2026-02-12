package com.example.scoi.domain.websocket.dto;

import java.util.Date;

public class UpbitResDTO {

    // 현제 코인 가격
    public record Ticker(
            String ty,
            String cd,
            Double op,
            Double hp,
            Double lp,
            Double tp,
            Double pcp,
            String c,
            Double cp,
            Double scp,
            Double cr,
            Double scr,
            Double tv,
            Double atv,
            Double atv24h,
            Double atp,
            Double atp24h,
            String tdt,
            String ttm,
            Long ttms,
            String ab,
            Double aav,
            Double abv,
            Double h52wp,
            String h52wdt,
            Double l52wp,
            String l52wdt,
            String ms,
            Date dd,
            Long tms,
            String st,

            // Deprecated
            Boolean its,
            String mw
    ){}
}
