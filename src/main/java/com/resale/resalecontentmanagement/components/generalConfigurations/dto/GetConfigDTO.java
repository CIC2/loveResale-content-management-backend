package com.resale.resalecontentmanagement.components.generalConfigurations.dto;

import com.resale.resalecontentmanagement.model.Configurations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetConfigDTO {
    private Integer id;
    private String configKey;
    private String configValue;
    private String conditionType;
    private String conditionValue;
    private Boolean isActive;
    private String description;

    public GetConfigDTO(Configurations configurations) {
        this.id = configurations.getId();
        this.configKey = configurations.getConfigKey();
        this.configValue = configurations.getConfigValue();
        this.conditionType = configurations.getConditionType();
        this.conditionValue = configurations.getConditionValue();
        this.isActive = configurations.getIsActive();
        this.description = configurations.getDescription();
    }
}


