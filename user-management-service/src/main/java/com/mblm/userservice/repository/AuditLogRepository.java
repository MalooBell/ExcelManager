package com.mblm.userservice.repository;

import com.mblm.userservice.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les interactions avec la table 'audit_logs'.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Spring Data JPA fournira les implémentations pour les opérations CRUD de base.
    // On pourra ajouter des méthodes de recherche personnalisées ici plus tard si nécessaire.
}