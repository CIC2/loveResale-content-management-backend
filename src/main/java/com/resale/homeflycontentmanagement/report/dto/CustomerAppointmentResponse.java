package com.resale.homeflycontentmanagement.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAppointmentResponse {
    private Integer id;
    private LocalDateTime registerDate;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String country;
    private String countryCode;
    private Integer age;
    private Integer appointmentId;
    private LocalDateTime appointmentDate;
    private boolean exist;
}


