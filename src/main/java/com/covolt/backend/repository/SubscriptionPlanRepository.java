package com.covolt.backend.repository;

import com.covolt.backend.model.SubscriptionPlan; // Entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // PK tipi UUID ise

import com.covolt.backend.model.enums.PlanStatus;


@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> { // PK tipini BaseEntity ile eşleştir

    // Plan adına (name) göre bulma (unique index var)
    Optional<SubscriptionPlan> findByName(String name);

    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);

    // Durumuna göre (PlanStatus) ve public olma durumuna göre planları listele (UI için)
    // List<SubscriptionPlan> findByStatusAndIsPublic(PlanStatus status, boolean isPublic);

    // Durumuna göre (PlanStatus) tüm planları getir
    // List<SubscriptionPlan> findByStatus(PlanStatus status);
}