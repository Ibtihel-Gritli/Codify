package com.codify.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.codify.app.model.User;
import com.codify.app.repository.UserRepository;
import java.util.*;

// @Service : cette classe contient toute la logique métier des utilisateurs
// Elle fait le lien entre les controllers et la base de données
@Service
public class UserService {

    // Repository qui parle directement à la table "users" dans MySQL
    // Spring injecte automatiquement grâce à @Autowired
    @Autowired
    private UserRepository userRepository;

    // ══════════════════════════════════════════════════
    // SAUVEGARDER UN UTILISATEUR
    // ══════════════════════════════════════════════════

    // Créer ou mettre à jour un utilisateur dans la base de données
    // Utilisé à l'inscription ET quand on met à jour les points/niveau
    public void saveUser(User user) {
        userRepository.save(user); // INSERT ou UPDATE automatique selon si l'id existe
    }

    // ══════════════════════════════════════════════════
    // VÉRIFIER SI UN EMAIL EXISTE
    // ══════════════════════════════════════════════════

    // Retourne true si l'email est déjà utilisé par quelqu'un
    // Utilisé à l'inscription pour éviter les doublons
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ══════════════════════════════════════════════════
    // CONNEXION UTILISATEUR
    // ══════════════════════════════════════════════════

    // Vérifier les identifiants de connexion
    // Retourne l'utilisateur si email + mdp sont corrects
    // Retourne null si incorrects
    public User loginUser(String email, String password) {

        // Chercher l'utilisateur par son email dans MySQL
        User user = userRepository.findByEmail(email);

        // Vérifier que l'utilisateur existe ET que le mot de passe correspond
        if (user != null && user.getPassword().equals(password)) {
            return user; // Connexion réussie
        }

        return null; // Connexion échouée
    }

    // ══════════════════════════════════════════════════
    // TROUVER UN UTILISATEUR PAR EMAIL
    // ══════════════════════════════════════════════════

    // Chercher un utilisateur par son email
    // Utilisé dans DefiController pour trouver le gagnant et lui donner des points
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ══════════════════════════════════════════════════
    // RÉCUPÉRER TOUS LES UTILISATEURS
    // ══════════════════════════════════════════════════

    // Retourner la liste complète de tous les utilisateurs
    // Utilisé dans DefiController pour trouver le gagnant par username
    // Utilisé dans AdminController pour afficher tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll(); // SELECT * FROM users
    }

    // ══════════════════════════════════════════════════
    // CLASSEMENT PAR NIVEAU
    // ══════════════════════════════════════════════════

    // Retourner le classement des utilisateurs d'un niveau donné
    // Trié par points décroissants (du plus grand au plus petit)
    // Utilisé dans ExerciceController pour /api/classement
    public List<Map<String, Object>> getClassementParNiveau(String niveau) {

        // Récupérer tous les utilisateurs
        List<User> tousUsers = userRepository.findAll();

        // Liste qui contiendra seulement les utilisateurs du niveau demandé
        List<Map<String, Object>> classement = new ArrayList<>();

        for (User user : tousUsers) {

            // Normaliser le niveau de l'utilisateur (enlever les accents)
            String niveauUser = user.getLevel() != null
                ? user.getLevel().toLowerCase()
                    .replace("é", "e")
                    .replace("è", "e")
                : "";

            // Normaliser le niveau recherché (enlever les accents)
            String niveauRecherche = niveau.toLowerCase()
                .replace("é", "e")
                .replace("è", "e");

            // Si le niveau correspond → ajouter à la liste
            if (niveauUser.equals(niveauRecherche)) {
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("username",          user.getUsername());
                ligne.put("points",            user.getPoints());
                ligne.put("exercicesCompletes", user.getExercicesCompletes());
                ligne.put("seriesCompletes",   user.getSeriesCompletes());
                classement.add(ligne);
            }
        }

        // Trier par points décroissants
        // Le joueur avec le plus de points est en premier
        classement.sort((a, b) ->
            Integer.compare(
                (int) b.get("points"),  // Points du joueur B
                (int) a.get("points")   // Points du joueur A
            )
        );

        // Retourner seulement les 20 premiers (top 20)
        return classement.subList(0, Math.min(20, classement.size()));
    }
}