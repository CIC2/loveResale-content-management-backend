package com.resale.resalecontentmanagement.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "view_sales_performance")
@Data
public class SalesPerformance {
    @Id
    private Integer id;

    @Column(name = "salesman_id")
    private Integer salesmanId;

    @Column(name = "salesman_name")
    private String salesmanName;

    @Column(name = "total_appointment")
    private Integer totalAppointment;

    @Column(name = "unit_sold")
    private Integer totalUnitSold;

    @Column(name = "total_amount")
    private Integer totalAmount;
}


