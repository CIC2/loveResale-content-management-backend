package com.resale.resalecontentmanagement.report.dto;

import com.resale.resalecontentmanagement.utils.PaginatedResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesmanAppointmentsReportResponse {
    private Integer totalAppointments;
    private Integer totalCompletedOffers;
    private PaginatedResponseDTO<SalesmanAppointmentResponse> appointments;
}


