package com.resale.resalecontentmanagement.components.adminModels.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderImagesDTO {

    private String folderName;
    private List<String> images;
    private List<IndexedImagesDTO> items;
    private List<FolderImagesDTO> unitPlans;
    private List<MImgsFolderDTO> mimgs;


    public static FolderImagesDTO forMimgs(String folderName, List<MImgsFolderDTO> mimgs) {
        FolderImagesDTO dto = new FolderImagesDTO();
        dto.setFolderName(folderName);
        dto.setMimgs(mimgs);
        return dto;
    }

    public FolderImagesDTO(String folderName, List<IndexedImagesDTO> items) {
        this.folderName = folderName;
        this.items = items;
    }
}

