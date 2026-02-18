package com.resale.resalecontentmanagement.components.adminModels.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexedImagesDTO {

    private Integer index;
    private String images;
    private List<UnitPlanDTO> unitPlans;

}


