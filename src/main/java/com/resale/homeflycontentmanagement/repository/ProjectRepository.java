package com.resale.homeflycontentmanagement.repository;

import com.resale.homeflycontentmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
}

