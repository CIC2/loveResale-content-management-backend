package com.resale.resalecontentmanagement.components.projectSections.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectSectionPostDTO {
    private Integer projectId;
    private String projectCode;
    private String sectionNumber;
    private String layout;
    private String title;
    private String subtitle;
    private String description;
    private String buttonUrl;
    private String videoUrl;

    // Images for this section
    private List<ProjectSectionImagePostDTO> images;
}


