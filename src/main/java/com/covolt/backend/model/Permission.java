package com.covolt.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // e.g. USER_CREATE, USER_DELETE

    @Column(length = 255)
    private String description;

    // İzinlerin rollerle ilişkisi
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;

    // Constructor, Getter, Setter Lombok ile sağlanıyor
}
