package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.view.OfferStatus;
import com.resale.resalecontentmanagement.model.view.SalesmanAppointmentView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SalesmanAppointmentRepository
        extends JpaRepository<SalesmanAppointmentView, Long> {

    @Query("""
    SELECT COUNT(v)
    FROM SalesmanAppointmentView v
    WHERE v.salesmanId = :salesmanId

""")
    Long countAppointments(
            Integer salesmanId
//            LocalDateTime fromDate,
//            LocalDateTime toDate
    );

    @Query("""
    SELECT COUNT(v)
    FROM SalesmanAppointmentView v
    WHERE v.salesmanId = :salesmanId
    AND v.offerStatus = :status

""")
    Long countOffersByStatus(
            @Param("salesmanId") Integer salesmanId,
            @Param("status") OfferStatus status
//            @Param("fromDate") LocalDateTime fromDate,
//            @Param("toDate") LocalDateTime toDate
    );


    @Query("""
    SELECT v
    FROM SalesmanAppointmentView v
    WHERE v.salesmanId = :salesmanId
    AND (:fromDate IS NULL OR v.appointmentDate >= :fromDate)
    AND (:toDate IS NULL OR v.appointmentDate <= :toDate)
    AND (:offerStatus IS NULL OR v.offerStatus = :offerStatus)
""")
    Page<SalesmanAppointmentView> findSalesmanAppointments(
            @Param("salesmanId") Integer salesmanId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("offerStatus") OfferStatus offerStatus,
            Pageable pageable
    );

}


