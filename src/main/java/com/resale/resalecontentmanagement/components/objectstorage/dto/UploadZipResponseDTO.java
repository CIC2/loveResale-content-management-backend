package com.resale.resalecontentmanagement.components.objectstorage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadZipResponseDTO {

    private String projectCode;
    private List<String> foldersBeingProcessed;
    private String status;
}



