package com.resale.homeflycontentmanagement.components.news;

import com.resale.homeflycontentmanagement.components.news.dto.NewsRequest;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReturnObject<?>> createNews(
            @RequestPart(name = "images", required = true) MultipartFile images,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate expiryDate,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(name = "mediaType", required = false) List<String> mediaTypes,
            @RequestParam(name = "mediaText", required = false) List<String> mediaTexts

    ) {
        try {
            NewsRequest newsRequest = new NewsRequest(title,description,expiryDate);
            var response = newsService.createNews(newsRequest, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(ex.getMessage(), false, null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>(
                            "Something went wrong while creating news",
                            false,
                            e.getMessage()
                    )
            );
        }
    }

    @PatchMapping(value = "/{newsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReturnObject<?>> updateNews(
            @PathVariable Integer newsId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate expiryDate,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer status,
            @RequestParam(name = "mediaType", required = false) List<String> mediaTypes,
            @RequestParam(name = "mediaText", required = false) List<String> mediaTexts,
            @RequestPart(value = "images", required = false) MultipartFile images,
            @RequestParam(value = "deletedIds", required = false) List<Integer> deletedIds
    ) {
        try {
            NewsRequest newsRequest = new NewsRequest(title,description,expiryDate, status);
            var response = newsService.updateNews(newsId, newsRequest, images, deletedIds);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ReturnObject<>(ex.getMessage(), false, null)
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>(ex.getMessage(), false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Something went wrong while patching news", false, e.getMessage())
            );
        }
    }

    @PatchMapping(value = "change-status/{newsId}")
    public ResponseEntity<ReturnObject<?>> updateNewsStatus(
            @PathVariable Integer newsId
    ) {
        try {
            var response = newsService.updateNewsStatus(newsId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ReturnObject<>(ex.getMessage(), false, null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Something went wrong while patching news", false, e.getMessage())
            );
        }
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<ReturnObject<?>> getNewsById(@PathVariable Integer newsId
    ) {
        try {
            return ResponseEntity.ok(newsService.getNewsById(newsId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch news", false, e.getMessage())
            );
        }
    }

    @GetMapping
    public ResponseEntity<ReturnObject<?>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, name = "status") Integer statusCode
    ) {
        try {
            return ResponseEntity.ok(
                    newsService.getNewsByStatus(page, size, statusCode)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ReturnObject<>("Invalid status code", false, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReturnObject<>("Failed to fetch news", false, e.getMessage())
            );
        }
    }

}


