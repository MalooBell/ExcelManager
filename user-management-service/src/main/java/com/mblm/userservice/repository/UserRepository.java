package com.mblm.userservice.repository;

import com.mblm.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Le Repository pour l'entité User.
 * Gère toutes les interactions avec la table 'users' de la base de données.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par son nom d'utilisateur.
     * Spring Data JPA crée automatiquement l'implémentation de cette méthode
     * en se basant sur son nom. La recherche n'est pas sensible à la casse.
     *
     * @param username Le nom d'utilisateur à rechercher.
     * @return un Optional contenant l'utilisateur s'il est trouvé, sinon un Optional vide.
     */
    Optional<User> findByUsername(String username);

}