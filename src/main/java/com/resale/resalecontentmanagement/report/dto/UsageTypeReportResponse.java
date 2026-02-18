package com.resale.resalecontentmanagement.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsageTypeReportResponse {


    private String typeCode;
    private String typeName;
    private String projectNameEn;
    private String projectNameAr;
    private Long viewCount;
}


