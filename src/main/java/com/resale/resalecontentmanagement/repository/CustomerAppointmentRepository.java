package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.view.CustomerAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerAppointmentRepository extends JpaRepository<CustomerAppointment,Integer> {
    @Query("""
    SELECT c
    FROM CustomerAppointment c
    WHERE (:countryCode IS NULL OR c.countryCode = :countryCode)
      AND (:country IS NULL OR c.country = :country)
      AND (:fullName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
""")
    Page<CustomerAppointment> getCustomers(
            @Param("countryCode") String countryCode,
            @Param("country") String country,
            @Param("fullName") String fullName,
            Pageable pageable
    );

}


