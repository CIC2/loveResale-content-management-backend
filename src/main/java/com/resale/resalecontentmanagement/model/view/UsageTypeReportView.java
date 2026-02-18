package com.resale.resalecontentmanagement.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_usage_types_report")
@Data
public class UsageTypeReportView {

    @Id
    private Integer id;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "project_name_en")
    private String projectNameEn;

    @Column(name = "project_name_ar")
    private String projectNameAr;

    @Column(name = "view_date")
    private LocalDateTime viewDate;

    @Column(name = "action_code")
    private Integer actionCode;

    @Column(name = "log_id")
    private Long logId;
}


