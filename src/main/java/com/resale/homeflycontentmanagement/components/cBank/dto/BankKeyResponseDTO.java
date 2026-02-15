package com.resale.homeflycontentmanagement.components.cBank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKeyResponseDTO {

    private Integer bankKeyId;
    private Integer bankId;
    private String bankName;
    private Integer projectId;
    private String projectName;
}


