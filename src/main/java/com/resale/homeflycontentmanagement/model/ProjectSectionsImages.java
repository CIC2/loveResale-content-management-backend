package com.resale.homeflycontentmanagement.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "project_sections_images", schema = "vso_dev_db", catalog = "")
public class ProjectSectionsImages {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "project_id")
    private int projectId;
    @Basic
    @Column(name = "project_code")
    private String projectCode;
    @Basic
    @Column(name = "section_id")
    private int sectionId;
    @Basic
    @Column(name = "image_url")
    private String imageUrl;
    @Basic
    @Column(name = "title")
    private String title;
    @Basic
    @Column(name = "subtitle")
    private String subtitle;
    @Basic
    @Column(name = "description")
    private String description;
    @Basic
    @Column(name = "title_ar")
    private String titleAr;
    @Basic
    @Column(name = "subtitle_ar")
    private String subtitleAr;
    @Basic
    @Column(name = "description_ar")
    private String descriptionAr;
}


