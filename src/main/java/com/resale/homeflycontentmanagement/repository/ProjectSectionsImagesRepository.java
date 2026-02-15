package com.resale.homeflycontentmanagement.repository;

import com.resale.homeflycontentmanagement.model.ProjectSectionsImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectSectionsImagesRepository extends JpaRepository<ProjectSectionsImages, Integer> {
    List<ProjectSectionsImages> findBySectionIdIn(List<Integer> sectionIds);
}

