package com.resale.resalecontentmanagement.components.adminModels;

import com.resale.resalecontentmanagement.components.adminModels.dto.ModelImagesResponseDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/internal")
public class InternalController {

    @Autowired
    private InternalService internalService;

    @GetMapping("modelMedia")
    public ResponseEntity<ReturnObject<ModelImagesResponseDTO>> getModelImages(
            @RequestParam String projectCode,
            @RequestParam String modelCode
    ) {

        ReturnObject<ModelImagesResponseDTO> response =
                internalService.getModelMedia(projectCode, modelCode);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}

