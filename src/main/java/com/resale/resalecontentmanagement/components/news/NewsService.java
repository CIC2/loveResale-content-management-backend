package com.resale.resalecontentmanagement.components.news;

import com.resale.resalecontentmanagement.model.News;
import com.resale.resalecontentmanagement.model.NewsImage;
import com.resale.resalecontentmanagement.model.NewsStatus;
import com.resale.resalecontentmanagement.components.news.dto.NewsRequest;
import com.resale.resalecontentmanagement.components.news.dto.NewsResponse;
import com.resale.resalecontentmanagement.components.objectstorage.StorageService;
import com.resale.resalecontentmanagement.repository.NewsImageRepository;
import com.resale.resalecontentmanagement.repository.NewsRepository;
import com.resale.resalecontentmanagement.utils.PageResponse;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;
    private final StorageService storageService;
    @Transactional
    public ReturnObject<NewsResponse> createNews(NewsRequest request, MultipartFile images) throws IOException {

        validateCreateRequest(request, images);
        News news = request.toNews();
        news.setStatus(NewsStatus.ACTIVE);
        News savedNews = newsRepository.save(news);

        int index = 1;


            String ext = images.getOriginalFilename() != null
                    ? images.getOriginalFilename().substring(images.getOriginalFilename().lastIndexOf('.') + 1)
                    : "jpg";

            String key = "news/" + savedNews.getId() +"/" + index + "." + ext;

            storageService.uploadMultipartFile(images, key);

            NewsImage image = NewsImage.builder()
                    .newsId(savedNews.getId())
                    .fileName(images.getOriginalFilename())
                    .contentType(images.getContentType())
                    .imageUrl(key) // store COS key or URL
                    .build();

            newsImageRepository.save(image);

        List<NewsImage> savedImages = newsImageRepository.findByNewsId(savedNews.getId());
        NewsResponse dto = NewsResponse.fromNews(savedNews, savedImages);

        return new ReturnObject<>("News created successfully", true, dto);
    }


    private void validateCreateRequest(NewsRequest request, MultipartFile images) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        if (request.getExpirationDate() != null &&
                request.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    @Transactional
    public ReturnObject<NewsResponse> updateNews(Integer id, NewsRequest request, MultipartFile images, List<Integer> deletedIds) throws IOException {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("News with id " + id + " not found"));

        News updatedNews = validateUpdateRequest(id, existingNews, request, images, deletedIds);

        var image = newsImageRepository.findByNewsId(id);
        var dto  = NewsResponse.fromNews(updatedNews, image);
        return new ReturnObject<>("News updated successfully", true, dto);
    }

    private News validateUpdateRequest(Integer newsId, News existingNews, NewsRequest request,
                                       MultipartFile image, List<Integer> deletedIds) throws IOException {

        boolean update = false;
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            existingNews.setTitle(request.getTitle());
            update = true;
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            existingNews.setDescription(request.getDescription());
            update = true;
        }
        if (request.getExpirationDate() != null) {
            existingNews.setExpirationDate(request.getExpirationDate());
            update = true;
        }
        if (deletedIds != null && !deletedIds.isEmpty()) {

            List<NewsImage> imgs = newsImageRepository.findByNewsId(newsId);

            if (imgs.size() == 0) {
                throw new IllegalArgumentException("No images exist to delete");
            }

            NewsImage img = imgs.get(0);

            storageService.deleteObject(img.getImageUrl());
            newsImageRepository.delete(img);

            update = true;
        }

        if (image != null && !image.isEmpty()) {

            List<NewsImage> oldImgs = newsImageRepository.findByNewsId(newsId);
            if (!oldImgs.isEmpty()) {
                storageService.deleteObject(oldImgs.get(0).getImageUrl());
                newsImageRepository.delete(oldImgs.get(0));
            }

            String ext = image.getOriginalFilename() != null ?
                    image.getOriginalFilename().substring(image.getOriginalFilename().lastIndexOf('.') + 1) : "jpg";


            String key = "news/" + newsId + "/1." + ext;

            storageService.uploadMultipartFile(image, key);

            NewsImage newImg = new NewsImage();
            newImg.setNewsId(newsId);
            newImg.setFileName(image.getOriginalFilename());
            newImg.setContentType(image.getContentType());
            newImg.setImageUrl(key);

            newsImageRepository.save(newImg);

            update = true;
        }
        if (request.getStatus() != null) {
            existingNews.setStatus(NewsStatus.fromCode(request.getStatus()));
            update = true;
        }
        if (!update)
            throw new IllegalArgumentException("No valid fields to update");
        return newsRepository.save(existingNews);
    }

    @Transactional
    public ReturnObject<NewsResponse> updateNewsStatus(Integer id) throws IOException {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("News with id " + id + " not found"));

        if (existingNews.getStatus() == NewsStatus.ACTIVE) {
            existingNews.setStatus(NewsStatus.INACTIVE);
        }else {
            existingNews.setStatus(NewsStatus.ACTIVE);
        }
        var image = newsImageRepository.findByNewsId(id);
        var dto  = NewsResponse.fromNews(existingNews, image);
        return new ReturnObject<>("News updated successfully", true, dto);
    }

    public ReturnObject<NewsResponse> getNewsById(Integer newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new EntityNotFoundException("News not found with id: " + newsId));

        List<NewsImage> images = newsImageRepository.findByNewsId(newsId);

        NewsResponse response = NewsResponse.fromNews(news, images);

        return new ReturnObject<>("News retrieved successfully", true, response);
    }

    public ReturnObject<?> getNewsByStatus(int page, int size, Integer statusCode) {

        page = Math.max(page, 0);
        size = (size <= 0 || size > 100) ? 10 : size;

        NewsStatus status = null;

        if (statusCode != null) {
            status = NewsStatus.fromCode(statusCode);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<News> newsPage = (status == null)
                ? newsRepository.findAll(pageable)
                : newsRepository.findByStatus(pageable, status);

        if (newsPage.isEmpty()) {
            var empty = PageResponse.<NewsResponse>builder()
                    .content(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            return new ReturnObject<>("No news found", true, empty);
        }

        List<Integer> ids = newsPage.getContent()
                .stream()
                .map(News::getId)
                .toList();

        List<NewsImage> allImages = newsImageRepository.findByNewsIdIn(ids);

        Map<Integer, List<NewsImage>> imagesByNews = allImages.stream()
                .collect(Collectors.groupingBy(NewsImage::getNewsId));

        List<NewsResponse> responses = newsPage.getContent().stream()
                .map(n -> NewsResponse.fromNews(
                        n,
                        imagesByNews.getOrDefault(n.getId(), List.of())
                ))
                .toList();

        var pageDto = PageResponse.<NewsResponse>builder()
                .content(responses)
                .page(newsPage.getNumber())
                .size(newsPage.getSize())
                .totalElements(newsPage.getTotalElements())
                .totalPages(newsPage.getTotalPages())
                .last(newsPage.isLast())
                .build();

        return new ReturnObject<>("All news retrieved successfully", true, pageDto);
    }
}


