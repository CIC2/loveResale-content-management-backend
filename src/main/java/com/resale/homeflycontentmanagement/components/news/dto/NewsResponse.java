package com.resale.homeflycontentmanagement.components.news.dto;

import com.resale.homeflycontentmanagement.model.News;
import com.resale.homeflycontentmanagement.model.NewsImage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {


    private Integer id;
    private String title;
    private String description;
    private String imageUrl;
    private LocalDate expirationDate;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Integer status;
    private List<NewsImageResponse> images;

    public static NewsResponse fromNews(News news, List<NewsImage> images) {
        NewsResponse dto = new NewsResponse();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setDescription(news.getDescription());
        dto.setExpirationDate(news.getExpirationDate());
        dto.setCreateAt(news.getCreatedAt());
        dto.setUpdateAt(news.getUpdatedAt());
        dto.setStatus(news.getStatus() != null ? news.getStatus().getCode() : null);
        if (images != null && !images.isEmpty()) {
            dto.setImages(images.stream().map(NewsImageResponse::fromNewsImage).toList());
            for(NewsImage image : images) {
                dto.setImageUrl(image.getImageUrl());
            }
        }
        return dto;
    }
    public NewsResponse(String title, Integer id, String description, Integer status, LocalDateTime updateAt, LocalDateTime createAt, LocalDate expirationDate, List<NewsImageResponse> images) {
        this.title = title;
        this.id = id;
        this.description = description;
        this.status = status;
        this.updateAt = updateAt;
        this.createAt = createAt;
        this.expirationDate = expirationDate;
        this.images = images;
    }
}


