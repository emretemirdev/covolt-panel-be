package com.covolt.backend.core.repository;

import com.covolt.backend.core.model.SubscriptionPlan; // Entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // PK tipi UUID ise


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