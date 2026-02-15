package com.resale.homeflycontentmanagement.components.generalConfigurations.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateConfigDTO {
    private Integer id;
    private String configKey;
    private String configValue;
    private String conditionType;
    private String conditionValue;
    private Boolean isActive;
    private String description;
}

