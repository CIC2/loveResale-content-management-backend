package com.resale.resalecontentmanagement.components.cBank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKeyDetailsResponseDTO {

    private Integer bankKeyId;
    private Integer bankId;
    private String bankName;
    private Integer projectId;
    private String projectName;

    private String accessKey;
    private String profileId;
    private String secretKey;
    private String extraKey;
    private Long lastChangeUserId;
}


