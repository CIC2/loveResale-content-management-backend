package com.resale.homeflycontentmanagement.components.adminModels;

import com.resale.homeflycontentmanagement.components.adminModels.dto.MImgsFolderDTO;
import com.resale.homeflycontentmanagement.components.adminModels.dto.ModelImageDTO;
import com.resale.homeflycontentmanagement.components.adminModels.dto.ModelImagesResponseDTO;
import com.resale.homeflycontentmanagement.components.adminModels.dto.UnitPlanDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/models")
public class UserModelController {

    @Autowired
    UserModelService userModelService;

    @GetMapping
    public ResponseEntity<ReturnObject<ModelImagesResponseDTO>> getModelImages(
            @RequestParam String projectCode,
            @RequestParam String modelCode
    ) {

        ReturnObject<ModelImagesResponseDTO> response =
                userModelService.getModelImages(projectCode, modelCode);
        if (response.getStatus() == null || !response.getStatus()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


    @GetMapping("/project")
    public ResponseEntity<ReturnObject<List<ModelImageDTO>>> getProjectModelsCoverImages(
            @RequestParam String projectCode
    ) {
        ReturnObject<List<ModelImageDTO>> response =
                userModelService.getProjectModelsImages(projectCode);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/unitPlansForFloor")
    public ResponseEntity<ReturnObject<List<UnitPlanDTO>>> getUnitPlansForFloor(
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) String floorNumber
    ) {
        ReturnObject<List<UnitPlanDTO>> unitPlansResponse = userModelService.getUnitPlansForFloor(projectCode, modelCode, floorNumber);

        if (unitPlansResponse.getStatus()) {
            return ResponseEntity.ok(unitPlansResponse);
        } else {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(unitPlansResponse);
        }
    }

    @GetMapping("/MImgs")
    public ReturnObject<List<MImgsFolderDTO>> getMainMImgs(
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String modelCode
    ) {
        return userModelService.getMImgs(projectCode, modelCode);
    }
}


