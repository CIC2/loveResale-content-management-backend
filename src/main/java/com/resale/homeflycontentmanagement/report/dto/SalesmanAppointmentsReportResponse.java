package com.resale.homeflycontentmanagement.report.dto;

import com.resale.homeflycontentmanagement.utils.PaginatedResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesmanAppointmentsReportResponse {
    private Integer totalAppointments;
    private Integer totalCompletedOffers;
    private PaginatedResponseDTO<SalesmanAppointmentResponse> appointments;
}


