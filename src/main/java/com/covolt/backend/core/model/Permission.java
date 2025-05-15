package com.covolt.backend.core.model;

import jakarta.persistence.*;
import lombok.*; // Getter, Setter, Builder, NoArgsConstructor, AllArgsConstructor
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true) // toBuilder = true EKLENDİ

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permissions")
public class Permission extends BaseEntity { // BaseEntity'den ID ve diğer alanları alacak

    @Column(nullable = false, unique = true)
    private String name; // İzin anahtarı, örn: "USER_CREATE", "REPORT_VIEW_ALL", "REACTIVE_PENALTY_VIEW"

    @Column(nullable = true)
    private String description;

    // Bir iznin hangi rollerde kullanıldığını görmek için (opsiyonel, genellikle Role tarafı yönetir)
    // Eğer Permission tarafından Role'leri yönetmek istemiyorsak bu alanı eklemeyebiliriz.
    // Genellikle @JoinTable ilişkide bir tarafın yönetmesi yeterlidir. Role tarafında tanımladık.
    // @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    // @Builder.Default
    // private Set<Role> roles = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission permission)) return false;
        if (!super.equals(o)) return false; // BaseEntity'deki ID karşılaştırmasını da yapsın
        return getName() != null ? getName().equals(permission.getName()) : permission.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}