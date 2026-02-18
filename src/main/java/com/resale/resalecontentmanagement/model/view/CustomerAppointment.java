package com.resale.resalecontentmanagement.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_customer_appointment")
@Data
public class CustomerAppointment {
    @Id
    private Integer id;
    private LocalDateTime registerDate;
    private String fullName;
    private String phoneNumber;
    private String country;
    private String countryCode;
    private String birthdate;
    private Integer appointment;
    private LocalDateTime appointmentDate;
    private boolean exist;

}


