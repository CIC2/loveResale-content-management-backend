package com.resale.homeflycontentmanagement.repository;
import com.resale.homeflycontentmanagement.model.CBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CBankRepository  extends JpaRepository<CBank, Integer> {

    List<CBank> findAll();
}

