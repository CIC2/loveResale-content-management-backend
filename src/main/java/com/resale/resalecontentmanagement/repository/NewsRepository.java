package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.News;
import com.resale.resalecontentmanagement.model.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Integer> {
    Page<News> findByStatus(Pageable pageable, NewsStatus status);

        @Query("""
        SELECT n FROM News n
        WHERE n.status = com.vso.tmgvsocontentmanagement.tmgvsocontentmanagement.model.NewsStatus.ACTIVE
        AND (n.expirationDate IS NULL OR n.expirationDate >= CURRENT_DATE)
        ORDER BY n.createdAt DESC
    """)

        Page<News> findActiveAndNotExpired(Pageable pageable);
    @Query("""
    SELECT n FROM News n
    WHERE n.id = :id
    AND n.status = com.vso.tmgvsocontentmanagement.tmgvsocontentmanagement.model.NewsStatus.ACTIVE
    AND (n.expirationDate IS NULL OR n.expirationDate >= CURRENT_DATE)
""")
    Optional<News> findActiveAndNotExpiredById(Integer id);

}



