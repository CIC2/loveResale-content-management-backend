package com.resale.resalecontentmanagement.report;

import com.resale.resalecontentmanagement.report.dto.*;
import com.resale.resalecontentmanagement.security.CheckPermission;
import com.resale.resalecontentmanagement.utils.PaginatedResponseDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/units")
    public ResponseEntity<ReturnObject<PaginatedResponseDTO<UnitReportResponse>>> getUnitsReport(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(
                    reportService.getUnitsReport(projectId, startDate, endDate, page, size)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(e.getMessage(), false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch units report", false, null)
            );
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ReturnObject<PaginatedResponseDTO<UsageTypeReportResponse>>> getTypesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(
                    reportService.getTypesReport(startDate, endDate, page, size)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(e.getMessage(), false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch usage types report", false, null)
            );
        }
    }

    @GetMapping("/salesman/appointments/{salesmanId}")
    public ResponseEntity<ReturnObject<SalesmanAppointmentsReportResponse>> getSalesmanAppointments(
            @PathVariable Integer salesmanId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer offerStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(
                    reportService.getSalesmanAppointments(
                            salesmanId, startDate, endDate, offerStatus, page, size
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(e.getMessage(), false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch salesman appointments report", false, null)
            );
        }
    }

    @GetMapping("/customers")
    @CheckPermission(value = {"admin:login"})
    public ResponseEntity<ReturnObject<PaginatedResponseDTO<CustomerAppointmentResponse>>> getCustomer(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(
                    reportService.getCustomers(
                            countryCode, country, name, page, size
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(e.getMessage(), false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch customers", false, null)
            );
        }
    }

    @GetMapping("/salesman/performance")
    public ResponseEntity<ReturnObject<PaginatedResponseDTO<SalesmanPerformanceResponse>>> getSalesmanPerformance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.ok(
                    reportService.getSalesmanPerformance(
                            page, size
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch salesman appointments report", false, null)
            );
        }
    }
}


