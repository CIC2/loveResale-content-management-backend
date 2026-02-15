package com.resale.homeflycontentmanagement.components.projectSections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionResponse;
import com.resale.homeflycontentmanagement.components.projectSections.dto.ProjectSectionsRequestDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/projectSections")
@RequiredArgsConstructor
public class ProjectSectionsController {

    @Autowired
    ProjectSectionsService projectSectionsService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReturnObject<?>> createProjectSections(
            @RequestParam MultiValueMap<String, MultipartFile> files,
            @RequestParam MultiValueMap<String, String> params
    ) {
        try {
            ReturnObject<?> result = projectSectionsService.createSections(params, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ReturnObject<>(e.getMessage(), false, null));
        }
    }




    @GetMapping("/{projectId}")
    public ResponseEntity<ReturnObject<List<ProjectSectionResponse>>> getProjectSectionsByProjectId(
            @PathVariable int projectId
    ) {
        try {
            List<ProjectSectionResponse> response = projectSectionsService.getSectionsByProjectId(projectId);
            ReturnObject<List<ProjectSectionResponse>> returnObject =
                    new ReturnObject<>("Project sections fetched successfully", true, response);
            return ResponseEntity.ok(returnObject);

        } catch (Exception e) {
            ReturnObject<List<ProjectSectionResponse>> returnObject =
                    new ReturnObject<>(e.getMessage(), false, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(returnObject);
        }
    }

    @PutMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReturnObject<?>> updateProjectSections(
            @PathVariable int projectId,
            @RequestParam MultiValueMap<String, MultipartFile> files,
            @RequestParam MultiValueMap<String, String> params
    ) {
        try {
            ReturnObject<?> result =
                    projectSectionsService.updateSections(projectId, params, files);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ReturnObject<>(e.getMessage(), false, null));
        }
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ReturnObject<?>> deleteProjectSections(@PathVariable int projectId, @RequestParam int sectionId) {
        try {
            ReturnObject<?> result = projectSectionsService.deleteSection(projectId, sectionId);
            return ResponseEntity.ok(result);
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ReturnObject<>(e.getMessage(), false, null));
        }
    }
}


