package com.resale.resalecontentmanagement.repository;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyDetailsResponseDTO;
import com.resale.resalecontentmanagement.components.cBank.dto.BankKeyResponseDTO;
import com.resale.resalecontentmanagement.model.CBankKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CBankKeysRepository  extends JpaRepository<CBankKeys, Integer> {

    @Query("""
        SELECT new com.resale.resalecontentmanagement.components.cBank.dto.BankKeyResponseDTO(
            k.id,
            b.id,
            b.bankName,
            p.id,
            p.nameEn
        )
        FROM CBankKeys k
        JOIN CBank b ON b.id = k.bankId
        JOIN Project p ON p.id = k.projectId
    """)
    List<BankKeyResponseDTO> findAllBankKeysWithDetails();

    CBankKeys findCBankKeysByProjectId(int projectId);

    @Query("""
    SELECT new com.resale.resalecontentmanagement.components.cBank.dto.BankKeyDetailsResponseDTO(
        k.id,
        b.id,
        b.bankName,
        p.id,
        p.nameEn,
        k.accessKey,
        k.profileId,
        k.secretKey,
        k.extraKey,
        k.lastChangeUserId
    )
    FROM CBankKeys k
    JOIN CBank b ON b.id = k.bankId
    JOIN Project p ON p.id = k.projectId
    WHERE k.id = :id
""")
    BankKeyDetailsResponseDTO findBankKeyDetailsById(Integer id);

}


