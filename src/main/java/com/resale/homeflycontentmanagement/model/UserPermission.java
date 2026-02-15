package com.resale.homeflycontentmanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_permission")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "permission_id", nullable = false)
    private Integer permissionId;
}


