package com.resale.homeflycontentmanagement.components.projectSections.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectSectionImageResponse {
    private String imageUrl;
    private String title;
    private String subtitle;
    private String description;
    private String titleAr;
    private String subtitleAr;
    private String descriptionAr;
}



