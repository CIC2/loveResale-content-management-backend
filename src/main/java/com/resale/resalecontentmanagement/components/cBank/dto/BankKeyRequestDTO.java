package com.resale.resalecontentmanagement.components.cBank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankKeyRequestDTO {
    private Integer bankId;
    private String accessKey;
    private String profileId;
    private String secretKey;
    private String extraKey;
    private Integer projectId;
}


