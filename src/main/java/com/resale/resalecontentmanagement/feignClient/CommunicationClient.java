package com.resale.resalecontentmanagement.feignClient;

import com.resale.resalecontentmanagement.components.objectstorage.dto.OtpMailDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "communication-ms", url = "${communication.service.url}")
public interface CommunicationClient {

    @PostMapping("/mail/user/sendMail")
    ReturnObject<String> sendOtpMail(@RequestBody OtpMailDTO otpMailDTO);
}

