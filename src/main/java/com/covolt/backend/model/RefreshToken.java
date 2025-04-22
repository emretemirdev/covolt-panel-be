package com.covolt.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false) // BaseEntity'nin alanlarını hashcode/equals dışında tut
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity { // Senin BaseEntity sınıfından miras al

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String username; // Genellikle kullanıcının e-posta adresi

    @Column(nullable = false)
    private Instant expiryDate;

    // BaseEntity'den gelen createdAt/updatedAt ve PK alanı (Long/UUID vs.) buraya dahil oluyor.
}