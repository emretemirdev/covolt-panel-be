package com.covolt.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return getName() != null && getName().equals(role.getName());
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
