package com.resale.homeflycontentmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "location_id", nullable = false)
    private Integer locationId;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "name_ar", length = 255)
    private String nameAr;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String descriptionAr;

    @Column(length = 255)
    private String location;

    @Column(length = 100)
    private String code;

    @Column(length = 1000)
    private String videoUrl;

    @Column (name = "company_code" , length = 255)
    private String companyCode;

    @Column(name = "coordinates_x")
    private Double coordinatesX;

    @Column(name = "coordinates_y")
    private Double coordinatesY;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "pdc")
    private byte pdc;
    @Column(name = "customer_type")
    private byte customerType;
    @Column(name = "bank_finance")
    private byte bankFinance;
    @Column(name = "vip_code")
    private byte vipCode;
    @Column(name = "currency_check")
    private byte currencyCheck;

}


