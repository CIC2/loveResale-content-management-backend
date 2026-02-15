package com.resale.homeflycontentmanagement.components.customerNews;

import com.resale.homeflycontentmanagement.components.customerNews.dto.NewsResponseDTO;
import com.resale.homeflycontentmanagement.model.News;
import com.resale.homeflycontentmanagement.model.NewsImage;
import com.resale.homeflycontentmanagement.components.news.dto.NewsImageResponse;
import com.resale.homeflycontentmanagement.repository.NewsImageRepository;
import com.resale.homeflycontentmanagement.repository.NewsRepository;
import com.resale.homeflycontentmanagement.utils.PaginatedResponseDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerNewsService {

    private final NewsRepository newsRepository;

    private final NewsImageRepository newsImageRepository;

    public ReturnObject<PaginatedResponseDTO<NewsResponseDTO>> getActiveNews(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsRepository.findActiveAndNotExpired(pageable);

        if (newsPage.isEmpty()) {
            return new ReturnObject<>(
                    "No active news found",
                    true,
                    new PaginatedResponseDTO<>(
                            List.of(),
                            page,
                            size,
                            0,
                            0,
                            true
                    )
            );
        }

        List<Integer> newsIds = newsPage.getContent()
                .stream()
                .map(News::getId)
                .toList();

        List<NewsImage> allImages = newsImageRepository.findByNewsIdIn(newsIds);

        Map<Integer, List<NewsImage>> imagesByNews =
                allImages.stream().collect(Collectors.groupingBy(NewsImage::getNewsId));

        List<NewsResponseDTO> content = newsPage.getContent()
                .stream()
                .map(n -> {
                    List<NewsImage> images = imagesByNews.getOrDefault(n.getId(), List.of());

                    return NewsResponseDTO.builder()
                            .id(n.getId())
                            .title(n.getTitle())
                            .description(n.getDescription())
                            .expirationDate(n.getExpirationDate())
                            .createdAt(n.getCreatedAt())
                            .images(
                                    images.stream()
                                            .map(NewsImageResponse::fromNewsImage)
                                            .toList()
                            )
                            .imageUrl(
                                    images.isEmpty() ? null : images.get(images.size() - 1).getImageUrl()
                            )
                            .build();
                })
                .toList();

        PaginatedResponseDTO<NewsResponseDTO> pageResponse =
                new PaginatedResponseDTO<>(
                        content,
                        newsPage.getNumber(),
                        newsPage.getSize(),
                        newsPage.getTotalElements(),
                        newsPage.getTotalPages(),
                        newsPage.isLast()
                );

        return new ReturnObject<>(
                "Active news retrieved successfully",
                true,
                pageResponse
        );
    }

    public ReturnObject<NewsResponseDTO> getNewsById(Integer id) {
        News news = newsRepository.findActiveAndNotExpiredById(id).orElse(null);

        if (news == null) {
            return new ReturnObject<>("News not found or expired", false, null);
        }

        List<NewsImage> images = newsImageRepository.findByNewsIdIn(List.of(id));

        NewsResponseDTO newsDTO = NewsResponseDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .description(news.getDescription())
                .expirationDate(news.getExpirationDate())
                .createdAt(news.getCreatedAt())
                .images(images.stream().map(NewsImageResponse::fromNewsImage).toList())
                .imageUrl(images.isEmpty() ? null : images.get(images.size() - 1).getImageUrl())
                .build();

        return new ReturnObject<>("Active news retrieved successfully", true, newsDTO);
    }
}

