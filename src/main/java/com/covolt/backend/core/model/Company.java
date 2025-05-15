package com.covolt.backend.core.model;

import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import jakarta.persistence.*;
import lombok.*; // Data, Builder, NoArgsConstructor, AllArgsConstructor, EqualsAndHashCode
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder(toBuilder = true) // toBuilder = true EKLENDİ

@Data // Getter, Setter, toString, equals, hashCode
@EqualsAndHashCode(callSuper = true, exclude = {"users", "companySubscriptions"}) // BaseEntity'den gelenleri dahil et, ilişkileri hariç tut
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "companies", indexes = { // Arama performansı için indexler
        @Index(name = "idx_companies_name", columnList = "name") // Firma adı genellikle benzersiz olmalı
})
public class Company extends BaseEntity { // BaseEntity'den miras alıyor (id, createdAt, updatedAt, version)

    @Column(nullable = false)
    private String name;

    @Column(name = "identifier", unique = true, nullable = true) // Vergi No, Ticaret Sicil No vb. Opsiyonel ama benzersiz olabilir.
    private String identifier; // Genel bir tanımlayıcı

    @Enumerated(EnumType.STRING) // Enum'u string olarak sakla
    @Column(name = "type", nullable = true) // Şirket, Okul, Hastane vb.
    private CompanyType type; // Bunun için bir CompanyType Enum tanımlayacağız (Opsiyonel ama önerilir)

    @Lob // Uzun metinler için (PostgreSQL'de TEXT tipine denk gelir)
    private String address; // Basit string veya daha sonra Embedded Address objesi

    @Column(name = "contact_email", length = 100) // İletişim e-postası için standart bir uzunluk
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default // Lombok Builder için default değer
    private CompanyStatus status = CompanyStatus.PENDING_VERIFICATION; // Yeni firmalar admin onayı bekleyebilir

    // Bir Firma'nın birden fazla kullanıcısı olabilir. İlişkinin ters tarafı (User'da company alanı)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    // Bir Firma'nın birden fazla abonelik kaydı (aktif/geçmiş) olabilir. İlişkinin ters tarafı (CompanySubscription'da company alanı)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CompanySubscription> companySubscriptions = new ArrayList<>(); // Veya Set<CompanySubscription>

    // Getter/Setter'lar ve diğer metotlar Lombok @Data tarafından sağlanır.
    // createdAt, updatedAt BaseEntity'den gelir.

    // İleride düşünülecek: Firmanın ayarları (JSONB veya ayrı tablo), logo URL'si vb.
}