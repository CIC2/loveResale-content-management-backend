package com.resale.homeflycontentmanagement.components.objectstorage;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobStatus {
    private String jobId;
    private String status; // PROCESSING, COMPLETED, FAILED
    private int uploadedCount;
    private String errorMessage;
    private LocalDateTime lastUpdated;
}


