package com.resale.homeflycontentmanagement.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "project_sections", schema = "vso_dev_db", catalog = "")
public class ProjectSections {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;
    @Basic
    @Column(name = "project_id")
    private Integer projectId;
    @Basic
    @Column(name = "project_code")
    private String projectCode;
    @Basic
    @Column(name = "section_number")
    private String sectionNumber;
    @Basic
    @Column(name = "layout")
    private String layout;
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
    @Basic
    @Column(name = "button_url")
    private String buttonUrl;
    @Basic
    @Column(name = "video_url")
    private String videoUrl;
    @Basic
    @Column(name = "logo")
    private String logo;
}


