package com.resale.resalecontentmanagement.components.news.dto;
import com.resale.resalecontentmanagement.model.News;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NewsRequest {

    private String title;
    private String description;
    private LocalDate expirationDate;
    private Integer status;

    public NewsRequest(String title, String description, LocalDate expirationDate, Integer status) {
        this.title = title;
        this.description = description;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    public NewsRequest(String title, String description, LocalDate expirationDate) {
        this.title = title;
        this.description = description;
        this.expirationDate = expirationDate;
    }


    public News toNews() {
        News entity = new News();
        entity.setTitle(this.title);
        entity.setDescription(this.description);
        entity.setExpirationDate(this.expirationDate);
        return entity;
    }
    
}


