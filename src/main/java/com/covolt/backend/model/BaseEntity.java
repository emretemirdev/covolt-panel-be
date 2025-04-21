package com.covolt.backend.model;

//Bu sınıf, tüm varlıkların (entity) ortak özelliklerini tanımlamak için kullanılan bir üst sınıftır.
// Bu sınıf, UUID türünde bir kimlik (id), oluşturulma tarihi (createdAt) ve güncellenme tarihi (updatedAt) içerir.
// Ayrıca, bu sınıfın alt sınıfları için oluşturulma ve güncellenme tarihlerini otomatik olarak ayarlamak için
// @PrePersist ve @PreUpdate anotasyonları ile işlevler tanımlanmıştır.
// Bu sınıf, JPA (Java Persistence API) kullanılarak veritabanı ile etkileşimde bulunmak için gerekli olan
// anotasyonları içerir. @MappedSuperclass anotasyonu, bu sınıfın bir üst sınıf olduğunu ve
// doğrudan bir varlık olarak kullanılmadığını belirtir. @Id anotasyonu, id alanının birincil anahtar olduğunu belirtir.
// @GeneratedValue anotasyonu, id alanının otomatik olarak oluşturulacağını belirtir.
// @UuidGenerator anotasyonu, id alanının UUID türünde otomatik olarak oluşturulacağını belirtir.
// @Column anotasyonu, alanların veritabanındaki özelliklerini tanımlar.
// @Getter ve @Setter anotasyonları, Lombok kütüphanesi tarafından sağlanan ve
// getter ve setter metodlarını otomatik olarak oluşturan anotasyonlardır.

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
