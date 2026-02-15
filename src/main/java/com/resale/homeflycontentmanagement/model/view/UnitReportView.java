package com.resale.homeflycontentmanagement.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_units_report")
@Data
public class UnitReportView {

    @Id
    private Integer id;

    @Column(name = "unit_code")
    private String unitCode;

    @Column(name = "usage_type_en")
    private String usageTypeEn;

    @Column(name = "usage_type_ar")
    private String usageTypeAr;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "project_name_en")
    private String projectNameEn;

    @Column(name = "project_name_ar")
    private String projectNameAr;

    @Column(name = "model_code")
    private String modelCode;

    @Column(name = "description")
    private String description;

    @Column(name = "view_date")
    private LocalDateTime viewDate;

    @Column(name = "action_code")
    private Integer actionCode;

    @Column(name = "log_id")
    private Long logId;
}


