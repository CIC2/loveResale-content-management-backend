package com.resale.resalecontentmanagement.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesmanPerformanceResponse {

    private Integer salesmanId;
    private String salesmanName;
    private Integer totalAppointment;
    private Integer totalUnitSold;
    private Integer totalAmount;
}

