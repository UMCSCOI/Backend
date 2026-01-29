package com.example.scoi.global.client;

import com.example.scoi.global.client.dto.CoolSmsDTO;
import com.example.scoi.global.config.CoolSmsConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "coolsms-client",
    url = "${coolsms.api-url}",
    configuration = CoolSmsConfig.class
)
public interface CoolSmsClient {

    @PostMapping("/messages/v4/send")
    CoolSmsDTO.SendResponse sendMessage(@RequestBody CoolSmsDTO.SendRequest request);
}
