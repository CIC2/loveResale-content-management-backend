package com.resale.homeflycontentmanagement.components.objectstorage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpMailDTO {
    String email;
    String otp;
    String mailSubject;
    String mailContent;
}


