package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.view.UsageTypeReportView;
import com.resale.resalecontentmanagement.report.dto.UsageTypeReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TypesReportRepository extends JpaRepository<UsageTypeReportView, Integer> {
    @Query("""
    SELECT new com.vso.tmgvsocontentmanagement.tmgvsocontentmanagement.report.dto.UsageTypeReportResponse(
        v.typeCode,
        v.typeName,
        v.projectNameEn,
        v.projectNameAr,
        COUNT(v.logId)
    )
    FROM UsageTypeReportView v
    WHERE (:startDate IS NULL OR v.viewDate >= :startDate)
    AND (:endDate IS NULL OR v.viewDate <= :endDate)
    GROUP BY v.typeCode, v.typeName, v.projectId
    ORDER BY COUNT(v.logId) DESC
""")
    Page<UsageTypeReportResponse> getUsageTypesReport(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}


