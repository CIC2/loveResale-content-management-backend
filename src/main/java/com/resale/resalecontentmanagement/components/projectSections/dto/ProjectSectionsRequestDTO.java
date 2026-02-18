package com.resale.resalecontentmanagement.components.projectSections.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectSectionsRequestDTO {
    private List<ProjectSectionPostDTO> sections;
}


