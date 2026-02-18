package com.resale.resalecontentmanagement.components.news.dto;

import com.resale.resalecontentmanagement.model.NewsImage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsImageResponse {

    private Integer id;
//    private String fileName;
    private String contentType;



    public static NewsImageResponse fromNewsImage(NewsImage newsImage) {
        NewsImageResponse dto = new NewsImageResponse();
        dto.setId(newsImage.getId());
//        dto.setFileName(newsImage.getFileName());
        dto.setContentType(newsImage.getContentType());
        return dto;
    }
}


