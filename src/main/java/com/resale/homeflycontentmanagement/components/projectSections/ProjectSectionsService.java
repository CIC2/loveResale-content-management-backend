package com.resale.homeflycontentmanagement.components.projectSections;

import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionImageResponse;
import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionPostDTO;
import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionResponse;
import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionsRequestDTO;
import com.resale.homeflycontentmanagement.model.Project;
import com.resale.homeflycontentmanagement.model.ProjectSections;
import com.resale.homeflycontentmanagement.model.ProjectSectionsImages;
import com.resale.homeflycontentmanagement.components.objectstorage.StorageService;
import com.resale.homeflycontentmanagement.repository.ProjectRepository;
import com.resale.homeflycontentmanagement.repository.ProjectSectionsImagesRepository;
import com.resale.homeflycontentmanagement.repository.ProjectSectionsRepository;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectSectionsService {

    private final ProjectSectionsRepository sectionsRepository;
    private final ProjectSectionsImagesRepository imagesRepository;
    private final StorageService storageService;
    private final ProjectRepository projectRepository;
    private static final Set<String> SECTION_FIELDS = Set.of(
            "layout", "title", "titleAr",
            "subtitle", "subtitleAr",
            "description", "descriptionAr",
            "videoUrl", "buttonUrl", "projectCode","logo"
    );

    private static final Set<String> IMAGE_FIELDS = Set.of(
            "file", "title", "titleAr",
            "subtitle", "subtitleAr",
            "description", "descriptionAr"
    );

    @Transactional
    public ReturnObject<?> createSections(
            MultiValueMap<String, String> params,
            MultiValueMap<String, MultipartFile> files
    ) {
        validateRequestKeys(params, files);

        // 1️⃣ Project ID and Project Code (ROOT params)
        Integer projectId = Integer.parseInt(params.getFirst("projectId"));
        String projectCode = params.getFirst("projectCode");  // NEW

        Map<Integer, ProjectSections> sectionMap = new HashMap<>();
        Map<String, Map<String, String>> imageMetadata = new HashMap<>();

        // 2️⃣ Parse section fields
        params.forEach((key, values) -> {
            if (key.equals("projectId") || key.equals("projectCode")) return;

            if (!key.matches("sections\\[\\d+]\\[[a-zA-Z]+]")) return;

            int index = extractSectionIndex(key);
            sectionMap.putIfAbsent(index, new ProjectSections());

            ProjectSections section = sectionMap.get(index);
            section.setProjectId(projectId);
            section.setProjectCode(projectCode);

            String field = extractFieldName(key);
            String value = values.get(0);

            switch (field) {
                case "layout" -> section.setLayout(value);
                case "title" -> section.setTitle(value);
                case "titleAr" -> section.setTitleAr(value);
                case "subtitle" -> section.setSubtitle(value);
                case "subtitleAr" -> section.setSubtitleAr(value);
                case "description" -> section.setDescription(value);
                case "descriptionAr" -> section.setDescriptionAr(value);
                case "buttonUrl" -> section.setButtonUrl(value);
                case "videoUrl" -> section.setVideoUrl(value);
            }
        });

        params.forEach((key, values) -> {
            if (key.matches("sections\\[\\d+]\\[images]\\[\\d+]\\[[a-zA-Z]+]")) {
                String imageKey = key.substring(0, key.lastIndexOf('['));
                String field = extractFieldName(key);

                imageMetadata.putIfAbsent(imageKey, new HashMap<>());
                imageMetadata.get(imageKey).put(field, values.get(0));
            }
        });



        files.forEach((key, fileList) -> {
            if (key.equals("logo")) return;
            int sectionIndex = extractSectionIndex(key);
            ProjectSections section = sectionMap.get(sectionIndex);

            MultipartFile file = fileList.get(0);
            String path = "projects/" + projectId +
                    "/sections/" + section.getId() + "/";

            try {
                storageService.uploadMultipartFile(
                        file,
                        path + file.getOriginalFilename()
                );
            } catch (IOException e) {
                throw new RuntimeException("Image upload failed", e);
            }

            ProjectSectionsImages image = new ProjectSectionsImages();
            image.setProjectId(projectId);
            image.setProjectCode(projectCode);
            image.setSectionId(section.getId());
            image.setImageUrl(path + file.getOriginalFilename());
            // Apply metadata if exists
            String imageKey = key.substring(0, key.lastIndexOf('['));
            Map<String, String> metadata = imageMetadata.get(imageKey);
            if (metadata != null) {
                image.setTitle(metadata.get("title"));
                image.setTitleAr(metadata.get("titleAr"));
                image.setSubtitle(metadata.get("subtitle"));
                image.setSubtitleAr(metadata.get("subtitleAr"));
                image.setDescription(metadata.get("description"));
                image.setDescriptionAr(metadata.get("descriptionAr"));
            }

            imagesRepository.save(image);
        });

        return new ReturnObject<>("Project sections processed successfully", true, null);
    }

    public List<ProjectSectionResponse> getSectionsByProjectId(int projectId) {

        List<ProjectSections> sections = sectionsRepository.findByProjectId(projectId);
        // Sort sections by ID to maintain consistent ordering
        sections.sort(Comparator.comparing(ProjectSections::getId));

        List<Integer> sectionIds = sections.stream().map(ProjectSections::getId).toList();
        Project project = projectRepository.findById(projectId).orElse(null);
        List<ProjectSectionsImages> images = imagesRepository.findBySectionIdIn(sectionIds);
        // Map sectionId -> list of images
        Map<Integer, List<ProjectSectionsImages>> imagesMap = images.stream()
                .collect(Collectors.groupingBy(ProjectSectionsImages::getSectionId));

        List<ProjectSectionResponse> responseList = new ArrayList<>();

        for (int index = 0; index < sections.size(); index++) {
            ProjectSections section = sections.get(index);

            ProjectSectionResponse sectionResp = new ProjectSectionResponse();
            sectionResp.setId(section.getId());
            sectionResp.setProjectId(section.getProjectId());
            sectionResp.setLogo(section.getLogo());
            sectionResp.setProjectCode(section.getProjectCode());
            sectionResp.setSectionNumber(String.valueOf(index));
            sectionResp.setLayout(section.getLayout());
            sectionResp.setTitle(section.getTitle());
            sectionResp.setSubtitle(section.getSubtitle());
            sectionResp.setDescription(section.getDescription());
            sectionResp.setTitleAr(section.getTitleAr());
            sectionResp.setSubtitleAr(section.getSubtitleAr());
            sectionResp.setDescriptionAr(section.getDescriptionAr());
            sectionResp.setButtonUrl(section.getButtonUrl());
            sectionResp.setVideoUrl(section.getVideoUrl());

            List<ProjectSectionImageResponse> imageResponses = new ArrayList<>();

            List<ProjectSectionsImages> sectionImages = imagesMap.getOrDefault(section.getId(), Collections.emptyList());

            for (ProjectSectionsImages img : sectionImages) {
                ProjectSectionImageResponse imgResp = new ProjectSectionImageResponse();
                imgResp.setImageUrl(img.getImageUrl());
                imgResp.setTitle(img.getTitle());
                imgResp.setTitleAr(img.getTitleAr());
                imgResp.setSubtitle(img.getSubtitle());
                imgResp.setSubtitleAr(img.getSubtitleAr());
                imgResp.setDescription(img.getDescription());
                imgResp.setDescriptionAr(img.getDescriptionAr());
                imageResponses.add(imgResp);
            }

            sectionResp.setImages(imageResponses);
            responseList.add(sectionResp);
        }

        return responseList;
    }

    @Transactional
    public ReturnObject<?> updateSections(
            int projectId,
            MultiValueMap<String, String> params,
            MultiValueMap<String, MultipartFile> files
    ) {
        validateRequestKeys(params, files);

        String projectCode = params.getFirst("projectCode");

        List<ProjectSections> existingSections = sectionsRepository.findByProjectId(projectId);
        existingSections.sort(Comparator.comparing(ProjectSections::getId));

        Map<Integer, ProjectSections> sectionMap = new HashMap<>();
        Map<String, Map<String, String>> imageMetadata = new HashMap<>();

        // Parse section fields
        params.forEach((key, values) -> {
            if (key.equals("projectId") || key.equals("projectCode")) return;

            if (!key.matches("sections\\[\\d+]\\[[a-zA-Z]+]")) return;

            int index = extractSectionIndex(key);

            // Get or create section based on index
            if (!sectionMap.containsKey(index)) {
                if (index < existingSections.size()) {
                    // Use existing section at this index
                    sectionMap.put(index, existingSections.get(index));
                } else {
                    // Create new section if index is beyond existing sections
                    ProjectSections newSection = new ProjectSections();
                    newSection.setProjectId(projectId);
                    if (projectCode != null) {
                        newSection.setProjectCode(projectCode);
                    }
                    sectionMap.put(index, newSection);
                }
            }

            ProjectSections section = sectionMap.get(index);

            // Set section number based on index
            section.setSectionNumber(String.valueOf(index));

            if (projectCode != null && section.getProjectCode() == null) {
                section.setProjectCode(projectCode);
            }

            String field = extractFieldName(key);
            String value = values.get(0);

            switch (field) {
                case "layout" -> section.setLayout(value);
                case "projectCode" -> section.setProjectCode(value);
                case "title" -> section.setTitle(value);
                case "titleAr" -> section.setTitleAr(value);
                case "subtitle" -> section.setSubtitle(value);
                case "subtitleAr" -> section.setSubtitleAr(value);
                case "description" -> section.setDescription(value);
                case "descriptionAr" -> section.setDescriptionAr(value);
                case "buttonUrl" -> section.setButtonUrl(value);
                case "videoUrl" -> section.setVideoUrl(value);
            }
        });

        // Parse image metadata
        params.forEach((key, values) -> {
            if (key.matches("sections\\[\\d+]\\[images]\\[\\d+]\\[[a-zA-Z]+]")) {
                String imageKey = key.substring(0, key.lastIndexOf('['));
                String field = extractFieldName(key);

                imageMetadata.putIfAbsent(imageKey, new HashMap<>());
                imageMetadata.get(imageKey).put(field, values.get(0));
            }
        });

        // Save all sections
        sectionMap.values().forEach(section -> {
            ProjectSections saved = sectionsRepository.save(section);
            section.setId(saved.getId());
        });

        // Handle logo uploads
        files.forEach((key, fileList) -> {
            if (key.matches("sections\\[\\d+]\\[logo]")) {
                if (fileList == null || fileList.isEmpty() ||
                        fileList.get(0) == null || fileList.get(0).isEmpty()) {
                    return;
                }

                int sectionIndex = extractSectionIndex(key);
                ProjectSections section = sectionMap.get(sectionIndex);

                if (section == null) {
                    throw new IllegalArgumentException("Logo without section: " + key);
                }

                if (!"1".equals(section.getLayout())) {
                    throw new IllegalArgumentException("Logo is only allowed for section with layout 1");
                }

                if (section.getLogo() != null) {
                    try {
                        storageService.deleteObject(section.getLogo());
                    } catch (Exception e) {
                        System.err.println("Failed to delete old logo: " + section.getLogo());
                    }
                }

                MultipartFile logoFile = fileList.get(0);
                String logoPath = "projects/" + projectId + "/sections/" + section.getId() + "/logo/";

                try {
                    storageService.uploadMultipartFile(logoFile, logoPath + logoFile.getOriginalFilename());
                    section.setLogo(logoPath + logoFile.getOriginalFilename());
                    sectionsRepository.save(section);
                } catch (IOException e) {
                    throw new RuntimeException("Logo upload failed", e);
                }
            }
        });

        // Group images by section
        Map<Integer, List<String>> imagesBySection = new HashMap<>();

        files.forEach((key, fileList) -> {
            if (key.matches("sections\\[\\d+]\\[logo]")) return; // Skip logos

            int sectionIndex = extractSectionIndex(key);
            imagesBySection.putIfAbsent(sectionIndex, new ArrayList<>());
            imagesBySection.get(sectionIndex).add(key);
        });

        // Delete existing images ONCE per section
        imagesBySection.keySet().forEach(sectionIndex -> {
            ProjectSections section = sectionMap.get(sectionIndex);
            if (section == null || section.getId() == 0) {
                throw new IllegalArgumentException("Image without valid section: " + sectionIndex);
            }

            List<ProjectSectionsImages> existingImages =
                    imagesRepository.findBySectionIdIn(List.of(section.getId()));

            for (ProjectSectionsImages img : existingImages) {
                try {
                    storageService.deleteObject(img.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Failed to delete image: " + img.getImageUrl());
                }
            }

            if (!existingImages.isEmpty()) {
                imagesRepository.deleteAll(existingImages);
            }
        });

        // Upload all new images
        files.forEach((key, fileList) -> {
            if (key.matches("sections\\[\\d+]\\[logo]")) return; // Skip logos

            int sectionIndex = extractSectionIndex(key);
            ProjectSections section = sectionMap.get(sectionIndex);

            if (section == null) {
                throw new IllegalArgumentException("Image without section: " + key);
            }

            MultipartFile file = fileList.get(0);
            String path = "projects/" + projectId +
                    "/sections/" + section.getId() + "/";

            try {
                storageService.uploadMultipartFile(
                        file,
                        path + file.getOriginalFilename()
                );
            } catch (IOException e) {
                throw new RuntimeException("Image upload failed", e);
            }

            ProjectSectionsImages image = new ProjectSectionsImages();
            image.setProjectId(projectId);
            image.setSectionId(section.getId());
            image.setImageUrl(path + file.getOriginalFilename());

            String imageKey = key.substring(0, key.lastIndexOf('['));
            Map<String, String> metadata = imageMetadata.get(imageKey);
            if (metadata != null) {
                image.setTitle(metadata.get("title"));
                image.setTitleAr(metadata.get("titleAr"));
                image.setSubtitle(metadata.get("subtitle"));
                image.setSubtitleAr(metadata.get("subtitleAr"));
                image.setDescription(metadata.get("description"));
                image.setDescriptionAr(metadata.get("descriptionAr"));
            }

            imagesRepository.save(image);
        });

        return new ReturnObject<>("Project sections updated successfully", true, null);
    }

    private String extractFieldName(String key) {
        return key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]"));
    }

    private int extractSectionIndex(String key) {
        return Integer.parseInt(
                key.substring(key.indexOf("[") + 1, key.indexOf("]"))
        );
    }

    private void validateRequestKeys(
            MultiValueMap<String, String> params,
            MultiValueMap<String, MultipartFile> files
    ) {
        if (params.containsKey("projectId") && params.get("projectId").size() != 1) {
            throw new IllegalArgumentException("Invalid projectId");
        }
        if (params.containsKey("projectCode") && params.get("projectCode").size() != 1) {
            throw new IllegalArgumentException("Invalid projectCode");
        }

        params.keySet().forEach(key -> {
            if (key.equals("projectId") || key.equals("projectCode")) return;

            if (key.matches("sections\\[\\d+]\\[[a-zA-Z]+]")) {
                String field = extractFieldName(key);
                if (!SECTION_FIELDS.contains(field)) {
                    throw new IllegalArgumentException("Invalid parameter: " + key);
                }
                return;
            }

            if (key.matches("sections\\[\\d+]\\[images]\\[\\d+]\\[[a-zA-Z]+]")) {
                String field = extractFieldName(key);
                if (!IMAGE_FIELDS.contains(field)) {
                    throw new IllegalArgumentException("Invalid parameter: " + key);
                }
                return;
            }

            throw new IllegalArgumentException("Invalid parameter: " + key);
        });

        files.keySet().forEach(key -> {
            if (key.matches("sections\\[\\d+]\\[logo]")) {
                List<MultipartFile> fileList = files.get(key);
                if (fileList != null && !fileList.isEmpty() &&
                        fileList.get(0) != null && !fileList.get(0).isEmpty()) {
                    int sectionIndex = extractSectionIndex(key);
                    String layout = params.getFirst("sections[" + sectionIndex + "][layout]");
                    if (layout != null && !"1".equals(layout)) {
                        throw new IllegalArgumentException("Logo is only allowed for section with layout 1, found layout: " + layout);
                    }
                }
                return;
            }
            if (!key.matches("sections\\[\\d+]\\[images]\\[\\d+]\\[file]")) {
                throw new IllegalArgumentException("Invalid file field: " + key);
            }
        });
    }

    @Transactional
    public ReturnObject<?> deleteSection(int projectId, int sectionId) {
        ProjectSections section = sectionsRepository
                .findByProjectIdAndId(projectId, sectionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Section not found: projectId=" + projectId + ", sectionId=" + sectionId
                ));

        List<ProjectSectionsImages> images = imagesRepository.findBySectionIdIn(List.of(sectionId));

        for (ProjectSectionsImages image : images) {
            try {
                storageService.deleteObject(image.getImageUrl());
                System.out.println("Deleted image from storage: " + image.getImageUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete image: " + image.getImageUrl() + " - " + e.getMessage());
            }
        }

        if (section.getLogo() != null && !section.getLogo().isEmpty()) {
            try {
                storageService.deleteObject(section.getLogo());
                System.out.println("Deleted logo from storage: " + section.getLogo());
            } catch (Exception e) {
                System.err.println("Failed to delete logo: " + section.getLogo() + " - " + e.getMessage());
            }
        }

        String sectionFolderPath = "projects/" + projectId + "/sections/" + sectionId + "/";
        try {
            storageService.deleteFolder(sectionFolderPath);
            System.out.println("Deleted section folder: " + sectionFolderPath);
        } catch (Exception e) {
            System.err.println("Failed to delete section folder: " + sectionFolderPath + " - " + e.getMessage());
        }

        if (!images.isEmpty()) {
            imagesRepository.deleteAll(images);
            System.out.println("Deleted " + images.size() + " image records from database");
        }

        sectionsRepository.delete(section);
        System.out.println("Deleted section from database: sectionId=" + sectionId);

        return new ReturnObject<>(
                "Section deleted successfully along with " + images.size() + " images",
                true,
                null
        );
    }
}

