package com.codify.app.repository;

import com.codify.app.model.HistoriqueExercice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<HistoriqueExercice, Long> {
    // Trouver tout l'historique d'un utilisateur trié par id décroissant
    List<HistoriqueExercice> findByUserIdOrderByIdDesc(Long userId);
}