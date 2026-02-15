package com.resale.homeflycontentmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "c_bank_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CBankKeys {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bank_id", nullable = false)
    private Integer bankId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(length = 1000)
    private String accessKey;

    @Column(length = 1000)
    private String profileId;

    @Column(length = 1000)
    private String secretKey;

    @Column(length = 1000)
    private String extraKey;

    @Column(name = "last_change_user_id", nullable = false)
    private Long lastChangeUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}


