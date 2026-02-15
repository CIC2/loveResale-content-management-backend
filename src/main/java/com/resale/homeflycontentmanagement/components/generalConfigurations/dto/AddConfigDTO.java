package com.resale.homeflycontentmanagement.components.generalConfigurations.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddConfigDTO {
    private String configKey;
    private String configValue;
    private String conditionType;
    private String conditionValue;
    private Boolean isActive;
    private String description;
}


