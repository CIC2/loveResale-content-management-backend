package com.resale.homeflycontentmanagement.components.adminModels.dto;

import com.resale.homeflycontentmanagement.components.adminModels.dto.FolderImagesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelImagesResponseDTO {
    private String projectCode;
    private String modelCode;
    private List<FolderImagesDTO> folders;
}


