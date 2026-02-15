package com.resale.homeflycontentmanagement.components.customerNews.dto;

import com.resale.homeflycontentmanagement.components.news.dto.NewsImageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewsResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDate expirationDate;
    private LocalDateTime createdAt;

    private String imageUrl;
    private List<NewsImageResponse> images;
}


