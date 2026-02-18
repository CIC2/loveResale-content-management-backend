package com.resale.resalecontentmanagement.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesmanAppointmentResponse {

    private String customerName;

    private LocalDateTime appointmentDate;

    private LocalDateTime offerDateTime;

    private LocalDate offerDate;

    private LocalTime offerTime;

    private String offerStatus;
}


