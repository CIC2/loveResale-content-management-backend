package com.resale.resalecontentmanagement.repository;

import com.resale.resalecontentmanagement.model.ProjectSectionsImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectSectionsImagesRepository extends JpaRepository<ProjectSectionsImages, Integer> {
    List<ProjectSectionsImages> findBySectionIdIn(List<Integer> sectionIds);
}

