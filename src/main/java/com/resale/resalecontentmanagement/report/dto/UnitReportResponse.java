package com.resale.resalecontentmanagement.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnitReportResponse {

    private String unitCode;
    private String usageTypeEn;
    private String usageTypeAr;
    private String projectNameEn;
    private String projectNameAr;
    private String modelCode;
    private String description;
    private Long viewCount;
}


