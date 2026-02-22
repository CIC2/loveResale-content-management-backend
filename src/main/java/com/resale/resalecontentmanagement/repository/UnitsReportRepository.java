package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.view.UnitReportView;
import com.resale.resalecontentmanagement.report.dto.UnitReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UnitsReportRepository extends JpaRepository<UnitReportView, Integer> {
    @Query("""
    SELECT new com.resale.resalecontentmanagement.report.dto.UnitReportResponse(
        v.unitCode,
        v.usageTypeEn,
        v.usageTypeAr,
        v.projectNameEn,
        v.projectNameAr,
        v.modelCode,
        v.description,
        COUNT(v.logId)
    )
    FROM UnitReportView v
    WHERE (:projectId IS NULL OR v.projectId = :projectId)
    AND (:startDate IS NULL OR v.viewDate >= :startDate)
    AND (:endDate IS NULL OR v.viewDate <= :endDate)
    GROUP BY v.unitCode, v.projectId, v.modelCode, v.description
    ORDER BY COUNT(v.logId) DESC
""")
    Page<UnitReportResponse> getUnitsReport(
            Integer projectId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}


