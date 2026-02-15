package com.resale.homeflycontentmanagement.model;

import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.AddConfigDTO;
import com.resale.homeflycontentmanagement.components.generalConfigurations.dto.UpdateConfigDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configurations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "config_key", nullable = false)
    private String configKey;
    @Column(name = "config_value")
    private String configValue;
    @Column(name = "condition_type")
    private String conditionType;
    @Column(name = "condition_value")
    private String conditionValue;
    @Column(name = "is_active")
    private Boolean isActive;
    @Column(name = "description")
    private String description;
    @Column(name = "created_at")
    private String createdAt;
    @Column(name = "updated_at")
    private String updatedAt;
    @Column(name = "last_modified_by")
    private Long lastModifiedBy;

    public Configurations(Long userId, AddConfigDTO addConfigDTO) {
        this.lastModifiedBy = userId;
        this.configKey = addConfigDTO.getConfigKey();
        this.configValue = addConfigDTO.getConfigValue();
        this.conditionType = addConfigDTO.getConditionType();
        this.conditionValue = addConfigDTO.getConditionValue();
        this.description = addConfigDTO.getDescription();
    }

}


