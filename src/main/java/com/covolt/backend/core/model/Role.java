package com.covolt.backend.core.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
@SuperBuilder(toBuilder = true) // toBuilder = true EKLENDİ

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // Örn: "ROLE_PLATFORM_ADMIN", "ROLE_COMPANY_ADMIN", "ROLE_USER"

    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY) // User entity'sindeki 'roles' alanına işaret eder
    @Builder.Default
    private Set<User> users = new HashSet<>();

    // YENİ EKLENEN KISIM: Role-Permission ilişkisi
    @ManyToMany(fetch = FetchType.EAGER) // Roller çekildiğinde izinlerini de getirmek yetkilendirme için genellikle mantıklıdır.
    // Performans kritikse LAZY + JOIN FETCH düşünülebilir.
    @JoinTable(
            name = "role_permissions", // Ara bağlantı tablosunun adı
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), // Bu entity'nin (Role) PK'sına referans veren kolon
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id") // Karşı entity'nin (Permission) PK'sına referans veren kolon
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        if (!super.equals(o)) return false; // BaseEntity ID karşılaştırması
        return getName() != null ? getName().equals(role.getName()) : role.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}