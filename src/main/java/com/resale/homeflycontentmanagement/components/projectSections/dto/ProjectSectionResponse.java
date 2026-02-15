package com.resale.homeflycontentmanagement.components.projectSections.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectSectionResponse {
    private int id;
    private int projectId;
    private String logo;
    private String projectCode;
    private String sectionNumber;
    private String layout;
    private String title;
    private String subtitle;
    private String description;
    private String titleAr;
    private String subtitleAr;
    private String descriptionAr;
    private String buttonUrl;
    private String videoUrl;
    private List<ProjectSectionImageResponse> images;
}


