package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.ProjectSections;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSectionsRepository extends JpaRepository<ProjectSections, Integer> {
    List<ProjectSections> findByProjectId(int projectId);


    Optional<ProjectSections> findByProjectIdAndId(int projectId, int id);
}

