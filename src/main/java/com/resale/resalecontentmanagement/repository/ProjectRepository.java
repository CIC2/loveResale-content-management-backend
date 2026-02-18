package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
}

