package com.resale.homeflycontentmanagement.components.generalConfigurations;

import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.AddConfigDTO;
import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.UpdateConfigDTO;
import com.resale.homeflycontentmanagement.security.user.CurrentUserId;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/generalAppConfiguration")
@RequiredArgsConstructor
public class GeneralConfigurationController {

    @Autowired
    GeneralConfigurationService generalConfigurationService;

    @PostMapping
    public ResponseEntity<ReturnObject<?>> createConfiguration(@CurrentUserId Long userId, @RequestBody AddConfigDTO addConfigDTO) {
        ReturnObject returnObject = generalConfigurationService.createConfiguration(userId, addConfigDTO);
        if (returnObject != null && returnObject.getStatus()) {
            return ResponseEntity.status(HttpStatus.OK).body(returnObject);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnObject);
    }

    @PutMapping("{configId}")
    public ResponseEntity<ReturnObject<?>> updateConfiguration(@CurrentUserId Long userId,
                                                               @PathVariable Integer configId,
                                                               @RequestBody UpdateConfigDTO updateConfigDTO) {
        ReturnObject returnObject = generalConfigurationService.updateConfiguration(userId, configId, updateConfigDTO);
        if (returnObject != null && returnObject.getStatus()) {
            return ResponseEntity.status(HttpStatus.OK).body(returnObject);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnObject);
    }

    @PutMapping
    public ResponseEntity<ReturnObject<?>> updateAllConfigurations(@CurrentUserId Long userId,
                                                                   @RequestBody List<UpdateConfigDTO> updateConfigDTO) {
        ReturnObject returnObject = generalConfigurationService.updateAllConfigurations(userId, updateConfigDTO);
        if (returnObject != null && returnObject.getStatus()) {
            return ResponseEntity.status(HttpStatus.OK).body(returnObject);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnObject);
    }

    @GetMapping
    public ResponseEntity<ReturnObject<?>> getAllConfigurations(@CurrentUserId Long userId) {
        ReturnObject returnObject = generalConfigurationService.findAllConfigurations(userId);
        if (returnObject != null && returnObject.getStatus()) {
            return ResponseEntity.status(HttpStatus.OK).body(returnObject);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnObject);
    }

}


