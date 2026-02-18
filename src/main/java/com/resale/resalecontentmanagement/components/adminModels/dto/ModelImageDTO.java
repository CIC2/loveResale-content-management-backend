package com.resale.resalecontentmanagement.components.adminModels.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelImageDTO {
    private String modelCode;
    private String folderName;
    private String image;
}


