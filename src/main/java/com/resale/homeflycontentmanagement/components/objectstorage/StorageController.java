package com.resale.homeflycontentmanagement.components.objectstorage;

import com.resale.homeflycontentmanagement.components.objectstorage.dto.ImagesDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/store")
public class StorageController {
    @Autowired
    private StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ReturnObject<String>> uploadImage(@RequestParam("file") MultipartFile file) {

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ReturnObject<>("Image File in Empty", false, null));
            }

            // Check if it's an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ReturnObject<>("Only image files are allowed", false, null)
                );
            }

            // Upload and get URL
            ReturnObject<String> result = storageService.uploadImage(file);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>("Image Upload Failed", false, e.getMessage()));
        }
    }
    @PostMapping("/uploadurl")
    public ResponseEntity<ReturnObject<String>> uploadImageFromUrl(@RequestParam("url") ImagesDTO dto) throws IOException {
        try {
            if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ReturnObject<>("Please provide a valid image URL",false,null));
            }

            String uploadedImageUrl = storageService.uploadImageFromUrl(dto);

            return ResponseEntity.ok(new ReturnObject<>("Image Uploaded Successfully",true,uploadedImageUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>("Image Upload Failed", false, e.getMessage()));
        }
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<ReturnObject<String>> deleteImage(@PathVariable String fileName) {
        try {
            storageService.deleteImage(fileName);
            return ResponseEntity.ok(new ReturnObject<>("Image Deleted Successfully",true,fileName));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReturnObject<>("Image Delete Failed", false, e.getMessage()));
        }
    }
}


