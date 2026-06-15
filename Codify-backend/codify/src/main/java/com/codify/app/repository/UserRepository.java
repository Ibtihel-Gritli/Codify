package com.codify.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codify.app.model.User;

// Repository = la couche qui parle directement à la base de données MySQL
// JpaRepository<User, Long> : gère la table User, dont l'id est de type Long
// Spring génère automatiquement : save(), findAll(), findById(), delete()...
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring crée automatiquement cette requête SQL :
    // SELECT * FROM users WHERE email = ?
    User findByEmail(String email);

    // Spring crée automatiquement :
    // SELECT COUNT(*) > 0 FROM users WHERE email = ?
    // Retourne true si l'email existe déjà
    boolean existsByEmail(String email);
}