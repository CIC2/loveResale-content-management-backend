package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.view.SalesPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesmanPerformanceRepository extends JpaRepository<SalesPerformance, Integer> {
}


