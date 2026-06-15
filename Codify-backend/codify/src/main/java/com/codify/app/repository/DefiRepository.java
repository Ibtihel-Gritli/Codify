package com.codify.app.repository;

import com.codify.app.model.Defi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DefiRepository extends JpaRepository<Defi, Long> {
    // Trouver les défis en attente d'un adversaire
    List<Defi> findByStatutOrderByIdDesc(String statut);

    // Trouver un défi par id
    Optional<Defi> findById(Long id);
}