package com.resale.resalecontentmanagement.report;

import com.resale.resalecontentmanagement.model.view.CustomerAppointment;
import com.resale.resalecontentmanagement.model.view.OfferStatus;
import com.resale.resalecontentmanagement.model.view.SalesPerformance;
import com.resale.resalecontentmanagement.model.view.SalesmanAppointmentView;
import com.resale.resalecontentmanagement.report.dto.*;
import com.resale.resalecontentmanagement.repository.*;
import com.resale.resalecontentmanagement.utils.PaginatedResponseDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UnitsReportRepository unitsReportRepository;
    private final TypesReportRepository typesReportRepository;
    private final SalesmanAppointmentRepository salesmanAppointmentRepository;
    private final CustomerAppointmentRepository customerAppointmentRepository;
    private final SalesmanPerformanceRepository salesmanPerformanceRepository;

    public ReturnObject<PaginatedResponseDTO<UnitReportResponse>> getUnitsReport(
            Integer projectId,
            LocalDateTime start,
            LocalDateTime end,
            Integer page,
            Integer size
    ) {
        validateDates(start, end);

        Page<UnitReportResponse> report =
                unitsReportRepository.getUnitsReport(
                        projectId,
                        start,
                        end,
                        PageRequest.of(page, size)
                );

        PaginatedResponseDTO<UnitReportResponse> data =
                new PaginatedResponseDTO<>(
                        report.getContent(),
                        report.getNumber(),
                        report.getSize(),
                        report.getTotalElements(),
                        report.getTotalPages(),
                        report.isLast()
                );

        return new ReturnObject<>(
                report.isEmpty() ? "No units found" : "Success",
                true,
                data
        );
    }

    public ReturnObject<PaginatedResponseDTO<UsageTypeReportResponse>> getTypesReport(LocalDateTime start, LocalDateTime end, Integer page, Integer size) {
        if (start != null && end != null && start.isAfter(end)) {
            return new ReturnObject<>("Start date must be before end date.", false, null);
        }
        if (start != null && end == null || (start == null && end != null)) {
            return new ReturnObject<>("You should enter both start date and end date.", false, null);
        }
        PageRequest pageable = PageRequest.of(page, size);
        Page<UsageTypeReportResponse> report = typesReportRepository.getUsageTypesReport(start, end, pageable);
        PaginatedResponseDTO<UsageTypeReportResponse> paginatedData = new PaginatedResponseDTO<>(report.getContent(), report.getNumber(), report.getSize(), report.getTotalElements(), report.getTotalPages(), report.isLast());
        if (paginatedData.getContent().isEmpty()) {
            return new ReturnObject<>("No units found", true, paginatedData);
        }
        return new ReturnObject<>("Success", true, paginatedData);

    }

    public ReturnObject<SalesmanAppointmentsReportResponse> getSalesmanAppointments(
            Integer salesmanId,
            LocalDateTime start,
            LocalDateTime end,
            Integer offerStatus,
            Integer page,
            Integer size
    ) {

        validateDates(start, end);

        OfferStatus offerStatusEnum = OfferStatus.fromId(offerStatus);

        Page<SalesmanAppointmentView> appointmentPage =
                salesmanAppointmentRepository.findSalesmanAppointments(
                        salesmanId,
                        start,
                        end,
                        offerStatusEnum,
                        PageRequest.of(page, size)
                );

        Page<SalesmanAppointmentResponse> responsePage =
                appointmentPage.map(this::mapSalesmanToResponse);

        PaginatedResponseDTO<SalesmanAppointmentResponse> paginated =
                new PaginatedResponseDTO<>(
                        responsePage.getContent(),
                        responsePage.getNumber(),
                        responsePage.getSize(),
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages(),
                        responsePage.isLast()
                );

        Long totalAppointments =
                salesmanAppointmentRepository.countAppointments(salesmanId);

        Long totalCompletedOffers =
                salesmanAppointmentRepository.countOffersByStatus(salesmanId, OfferStatus.COMPLETED);

        SalesmanAppointmentsReportResponse response =
                new SalesmanAppointmentsReportResponse(
                        totalAppointments.intValue(),
                        totalCompletedOffers.intValue(),
                        paginated
                );

        return new ReturnObject<>(
                paginated.getContent().isEmpty()
                        ? "No appointments found"
                        : "Success",
                true,
                response
        );
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if ((start == null && end != null) || (start != null && end == null)) {
            throw new IllegalArgumentException("You should enter both start date and end date");
        }
    }

    private SalesmanAppointmentResponse mapSalesmanToResponse(SalesmanAppointmentView view) {

        LocalDateTime offerDateTime = view.getOfferDateTime();
        OfferStatus status = view.getOfferStatus();

        return new SalesmanAppointmentResponse(
                view.getCustomerName(),
                view.getAppointmentDate(),
                offerDateTime,
                offerDateTime != null ? offerDateTime.toLocalDate() : null,
                offerDateTime != null ? offerDateTime.toLocalTime() : null,
                status != null ? status.getLabel() : null
        );
    }

    public ReturnObject<PaginatedResponseDTO<CustomerAppointmentResponse>> getCustomers(String countryCode, String country, String name, int page, int size) {
        Page<CustomerAppointment> customers =
                customerAppointmentRepository.getCustomers(
                        normalizeCountryCode(countryCode),
                        country,
                        name,
                        PageRequest.of(page, size)
                );

        Page<CustomerAppointmentResponse> report =
                customers.map(this::mapCustomerAppointmentToResponse);


        PaginatedResponseDTO<CustomerAppointmentResponse> data =
                new PaginatedResponseDTO<>(
                        report.getContent(),
                        report.getNumber(),
                        report.getSize(),
                        report.getTotalElements(),
                        report.getTotalPages(),
                        report.isLast()
                );

        return new ReturnObject<>(
                report.isEmpty() ? "No customer found" : "Success",
                true,
                data
        );
    }

    private CustomerAppointmentResponse mapCustomerAppointmentToResponse(CustomerAppointment customerAppointment) {

        String firstName = Optional.ofNullable(customerAppointment.getFullName())
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.split("\\s+")[0])
                .orElse(null);

        String lastName = Optional.ofNullable(customerAppointment.getFullName())
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> {
                    String[] parts = name.split("\\s+");
                    return parts[parts.length - 1];
                })
                .orElse(null);

        Integer age = calculateAge(customerAppointment.getBirthdate());


        return new CustomerAppointmentResponse(
                customerAppointment.getId(),
                customerAppointment.getRegisterDate(),
                firstName,
                lastName,
                customerAppointment.getPhoneNumber(),
                customerAppointment.getCountry(),
                customerAppointment.getCountryCode(),
                age,
                customerAppointment.getAppointment(),
                customerAppointment.getAppointmentDate(),
                customerAppointment.isExist()
                        );
    }

    private Integer calculateAge(String birthdateStr) {

        if (birthdateStr == null || birthdateStr.isBlank()) {
            return null;
        }

        try {
            LocalDate birthDate = LocalDate.parse(birthdateStr);
            return Period.between(birthDate, LocalDate.now()).getYears();

        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String normalizeCountryCode(String countryCode) {

        if (countryCode == null) {
            return null;
        }

        String normalized = countryCode.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }

        return normalized;
    }


    public ReturnObject<PaginatedResponseDTO<SalesmanPerformanceResponse>> getSalesmanPerformance(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<SalesPerformance> report = salesmanPerformanceRepository.findAll(pageable);
        Page<SalesmanPerformanceResponse> responsePage = report.map(this::mapSalesmanPerformanceToResponse);
        PaginatedResponseDTO<SalesmanPerformanceResponse> paginatedData = new PaginatedResponseDTO<>(responsePage.getContent(), responsePage.getNumber(), responsePage.getSize(), responsePage.getTotalElements(), responsePage.getTotalPages(), responsePage.isLast());
        if (paginatedData.getContent().isEmpty()) {
            return new ReturnObject<>("No salesman found", true, paginatedData);
        }
        return new ReturnObject<>("Success", true, paginatedData);


    }

    private SalesmanPerformanceResponse mapSalesmanPerformanceToResponse(SalesPerformance salesPerformance) {
        return new SalesmanPerformanceResponse(
                salesPerformance.getSalesmanId(),
                salesPerformance.getSalesmanName(),
                salesPerformance.getTotalAppointment(),
                salesPerformance.getTotalUnitSold(),
                salesPerformance.getTotalAmount()
        );
    }
}


