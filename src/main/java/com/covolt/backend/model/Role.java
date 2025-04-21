package com.covolt.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // e.g. ROLE_ADMIN, ROLE_USER

    @Column(length = 255)
    private String description;

    // Rollerin kullanıcılarla ilişkisi
    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

    // Rollerin izinlerle ilişkisi
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;

    // Constructor, Getter, Setter Lombok ile sağlanıyor
}
