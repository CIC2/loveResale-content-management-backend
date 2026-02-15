package com.resale.homeflycontentmanagement.model.view;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "view_sales_man_appointment_report")
@Data
public class SalesmanAppointmentView {

    @Id
    private Integer id;

    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "salesman_id")
    private Integer salesmanId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "appointment_date")
    private LocalDateTime appointmentDate;

    @Column(name = "offer_date_time")
    private LocalDateTime offerDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status")
    private OfferStatus offerStatus;

}


