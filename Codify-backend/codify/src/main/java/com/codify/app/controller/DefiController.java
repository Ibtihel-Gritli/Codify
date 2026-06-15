package com.codify.app.controller;

import com.codify.app.model.Defi;
import com.codify.app.model.User;
import com.codify.app.repository.DefiRepository;
import com.codify.app.service.DefiService;
import com.codify.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

// @Controller : gère toutes les routes liées aux défis/hackathons
@Controller
public class DefiController {

    // Service IA : génère les problèmes et évalue les solutions
    @Autowired
    private DefiService defiService;

    // Repository : accès direct à la table "defis" dans MySQL
    @Autowired
    private DefiRepository defiRepository;

    // Service utilisateurs : gère les points, niveaux, etc.
    @Autowired
    private UserService userService;

    // WebSocket : envoie des messages en temps réel aux 2 joueurs
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ══════════════════════════════════════════════════
    // PAGE PRINCIPALE DES DÉFIS
    // ══════════════════════════════════════════════════

    // Affiche la page defis.html → /defis
    @GetMapping("/defis")
    public String defisPage(HttpSession session, Model model) {

        // Récupérer l'utilisateur connecté depuis la session
        User user = (User) session.getAttribute("user");

        // Si pas connecté → rediriger vers login
        if (user == null) return "redirect:/login";

        // Récupérer tous les défis en attente d'un adversaire
        List<Defi> defisAttente =
            defiRepository.findByStatutOrderByIdDesc("attente");

        // Envoyer les données à la page HTML
        model.addAttribute("username",     user.getUsername());
        model.addAttribute("niveau",       user.getLevel());
        model.addAttribute("points",       user.getPoints());
        model.addAttribute("defisAttente", defisAttente);

        return "defis"; // → templates/defis.html
    }

    // ══════════════════════════════════════════════════
    // CRÉER UN NOUVEAU DÉFI
    // ══════════════════════════════════════════════════

    // Route : POST /api/defis/creer
    // Appelé quand l'utilisateur clique "Lancer le Hackathon"
    @PostMapping("/api/defis/creer")
    @ResponseBody // Retourne du JSON
    public Map<String, Object> creerDefi(
            @RequestParam String niveau,      // Niveau : debutant/intermediaire/avance
            @RequestParam String langage,     // Langage : python/java/c
            @RequestParam int dureeMinutes,   // Durée : 5/10/15/30 minutes
            HttpSession session) {

        // Vérifier que l'utilisateur est connecté
        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Demander à l'IA de générer un vrai problème de hackathon
        String probleme = defiService.genererProblemeDefi(niveau, langage);

        // Créer et remplir l'objet Defi
        Defi defi = new Defi();
        defi.setJoueur1(user.getUsername()); // Créateur = joueur 1
        defi.setNiveau(niveau);
        defi.setLangage(langage);
        defi.setProbleme(probleme);
        defi.setStatut("attente");           // En attente d'adversaire
        defi.setDureeMinutes(dureeMinutes);

        // Sauvegarder dans MySQL
        defiRepository.save(defi);

        // Notifier tous les utilisateurs connectés qu'un défi est disponible
        messagingTemplate.convertAndSend("/topic/defis",
            Map.of("type", "nouveau_defi", "defiId", defi.getId()));

        // Retourner les infos du défi créé
        return Map.of(
            "defiId",   defi.getId(),
            "probleme", probleme,
            "message",  "Hackathon créé ! En attente d'un adversaire..."
        );
    }

    // ══════════════════════════════════════════════════
    // REJOINDRE UN DÉFI EXISTANT
    // ══════════════════════════════════════════════════

    // Route : POST /api/defis/rejoindre
    // Appelé quand un utilisateur clique "Rejoindre ce Hackathon"
    @PostMapping("/api/defis/rejoindre")
    @ResponseBody
    public Map<String, Object> rejoindreDefi(
            @RequestParam Long defiId,
            HttpSession session) {

        // Vérifier que l'utilisateur est connecté
        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Chercher le défi dans MySQL
        Defi defi = defiRepository.findById(defiId).orElse(null);

        // Vérifications avant de rejoindre
        if (defi == null)
            return Map.of("erreur", "Défi introuvable");
        if (!defi.getStatut().equals("attente"))
            return Map.of("erreur", "Ce défi a déjà commencé");
        if (defi.getJoueur1().equals(user.getUsername()))
            return Map.of("erreur", "Vous ne pouvez pas rejoindre votre propre défi");

        // Mettre à jour le défi
        defi.setJoueur2(user.getUsername()); // Joueur 2 = celui qui rejoint
        defi.setStatut("en_cours");          // Le défi commence !

        // Enregistrer le timestamp EXACT du début
        // Ce timestamp sera envoyé aux 2 joueurs pour synchroniser leurs timers
        long timestampDebut = System.currentTimeMillis();
        defi.setTimestampDebut(timestampDebut);

        // Sauvegarder dans MySQL
        defiRepository.save(defi);

        // Notifier les 2 joueurs via WebSocket que le défi commence
        // Les 2 reçoivent le MÊME timestampDebut → même chronomètre
        messagingTemplate.convertAndSend("/topic/defi/" + defiId,
            Map.of(
                "type",           "defi_commence",
                "joueur1",        defi.getJoueur1(),
                "joueur2",        defi.getJoueur2(),
                "dureeMinutes",   defi.getDureeMinutes(),
                "timestampDebut", timestampDebut  // ← Synchronisation des timers
            )
        );

        // Retourner les infos pour ouvrir l'arène côté joueur 2
        return Map.of(
            "defiId",         defiId,
            "probleme",       defi.getProbleme(),
            "joueur1",        defi.getJoueur1(),
            "joueur2",        defi.getJoueur2(),
            "dureeMinutes",   defi.getDureeMinutes(),
            "timestampDebut", timestampDebut  // ← Même timestamp pour joueur 2
        );
    }

    // ══════════════════════════════════════════════════
    // SOUMETTRE UNE SOLUTION
    // ══════════════════════════════════════════════════

    // Route : POST /api/defis/soumettre
    // Appelé quand un joueur clique "Soumettre ma solution"
    @PostMapping("/api/defis/soumettre")
    @ResponseBody
    public Map<String, Object> soumettreDefi(
            @RequestParam Long defiId,
            @RequestParam String solution,
            @RequestParam long tempsSecondes, // Temps que le joueur a mis
            HttpSession session) {

        // Vérifier que l'utilisateur est connecté
        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Chercher le défi
        Defi defi = defiRepository.findById(defiId).orElse(null);
        if (defi == null) return Map.of("erreur", "Défi introuvable");

        // Vérifier que le défi est encore en cours
        // (éviter double soumission ou soumission après fin)
        if (!defi.getStatut().equals("en_cours")) {
            return Map.of("erreur", "Ce défi est déjà terminé");
        }

        // ── Évaluer la solution via l'IA ──
        // L'IA donne : score/100, complexité, optimisation, verdict
        Map<String, Object> evaluation = defiService.evaluerSolution(
            defi.getProbleme(),    // Le problème posé
            solution,              // La solution du joueur
            defi.getNiveau(),      // Son niveau
            defi.getLangage(),     // Le langage utilisé
            tempsSecondes,         // Temps de résolution
            defi.getDureeMinutes() // Durée totale du défi
        );

        // Récupérer le score calculé par l'IA
        int     score   = (int)     evaluation.get("score");
        boolean accepte = (boolean) evaluation.get("accepte");

        // Identifier quel joueur soumet (joueur 1 ou joueur 2)
        boolean estJoueur1 = defi.getJoueur1().equals(user.getUsername());

        // Mettre à jour le score et le temps du bon joueur
        if (estJoueur1) {
            defi.setScoreJoueur1(score);
            defi.setTempsJoueur1(tempsSecondes);
        } else {
            defi.setScoreJoueur2(score);
            defi.setTempsJoueur2(tempsSecondes);
        }

        // Notifier l'adversaire en temps réel
        messagingTemplate.convertAndSend("/topic/defi/" + defiId,
            Map.of(
                "type",          "solution_soumise",
                "joueur",        user.getUsername(),
                "score",         score,
                "accepte",       accepte,
                "tempsSecondes", tempsSecondes
            )
        );

        // ══════════════════════════════════════════════════
        // CAS 1 : UN JOUEUR TERMINE EN PREMIER
        // → Le défi s'arrête immédiatement pour les 2
        // ══════════════════════════════════════════════════
        // Si la solution est acceptée (score >= 60) → défi terminé !
        if (accepte && defi.getStatut().equals("en_cours")) {

            // Terminer immédiatement le défi
            String gagnant = user.getUsername(); // Celui qui termine en premier gagne
            defi.setGagnant(gagnant);
            defi.setStatut("termine");

            // Donner +50 points au gagnant
            donnerPointsGagnant(gagnant);

            // Comparer les 2 solutions via l'IA pour le bilan final
            String evalComparative = defiService.comparerDeuxSolutions(
                defi.getProbleme(),
                solution,
                defi.getJoueur1(),
                defi.getScoreJoueur1(),
                "(pas encore soumis)",
                defi.getJoueur2() != null ? defi.getJoueur2() : "?",
                defi.getScoreJoueur2(),
                defi.getTempsJoueur1(),
                defi.getTempsJoueur2()
            );

            // Sauvegarder dans MySQL
            defiRepository.save(defi);

            // Notifier les 2 joueurs que le défi est terminé
            // L'adversaire verra aussi le résultat final automatiquement
            messagingTemplate.convertAndSend("/topic/defi/" + defiId,
                Map.of(
                    "type",         "defi_termine",  // ← Arrêt immédiat pour les 2
                    "gagnant",      gagnant,
                    "scoreJoueur1", defi.getScoreJoueur1(),
                    "scoreJoueur2", defi.getScoreJoueur2(),
                    "joueur1",      defi.getJoueur1(),
                    "joueur2",      defi.getJoueur2() != null
                                    ? defi.getJoueur2() : "?",
                    "evaluation",   evalComparative,
                    "raison",       "premier_termine" // Raison de la fin
                )
            );

            // Ajouter les infos de fin dans la réponse JSON
            evaluation.put("defiTermine", true);
            evaluation.put("gagnant",     gagnant);
            evaluation.put("raison",      "premier_termine");
            return evaluation;
        }

        // ══════════════════════════════════════════════════
        // CAS 2 : LES 2 JOUEURS ONT SOUMIS (même si incorrects)
        // → Comparer les 2 solutions et désigner le gagnant
        // ══════════════════════════════════════════════════
        boolean joueur1Soumis = estJoueur1 || defi.getScoreJoueur1() > 0;
        boolean joueur2Soumis = !estJoueur1 || defi.getScoreJoueur2() > 0;

        if (joueur1Soumis && joueur2Soumis) {

            // Comparer les 2 solutions via l'IA
            String evalComparative = defiService.comparerDeuxSolutions(
                defi.getProbleme(),
                "", defi.getJoueur1(), defi.getScoreJoueur1(),
                "", defi.getJoueur2() != null ? defi.getJoueur2() : "?",
                defi.getScoreJoueur2(),
                defi.getTempsJoueur1(),
                defi.getTempsJoueur2()
            );

            // Déterminer le gagnant selon les scores et les temps
            String gagnant = determinerGagnant(defi);
            defi.setGagnant(gagnant);
            defi.setStatut("termine");

            // Donner +50 points au gagnant
            donnerPointsGagnant(gagnant);

            // Sauvegarder dans MySQL
            defiRepository.save(defi);

            // Notifier les 2 joueurs du résultat final
            messagingTemplate.convertAndSend("/topic/defi/" + defiId,
                Map.of(
                    "type",         "defi_termine",
                    "gagnant",      gagnant,
                    "scoreJoueur1", defi.getScoreJoueur1(),
                    "scoreJoueur2", defi.getScoreJoueur2(),
                    "joueur1",      defi.getJoueur1(),
                    "joueur2",      defi.getJoueur2() != null
                                    ? defi.getJoueur2() : "?",
                    "evaluation",   evalComparative,
                    "raison",       "deux_soumis" // Raison de la fin
                )
            );

            evaluation.put("defiTermine", true);
            evaluation.put("gagnant",     gagnant);
            evaluation.put("raison",      "deux_soumis");
            return evaluation;
        }

        // Si seulement un joueur a soumis → attendre l'adversaire
        defiRepository.save(defi);
        evaluation.put("defiTermine", false);
        evaluation.put("gagnant",     "");
        return evaluation;
    }

    // ══════════════════════════════════════════════════
    // CAS 3 : TEMPS ÉCOULÉ → FIN AUTOMATIQUE
    // ══════════════════════════════════════════════════

    // Route : POST /api/defis/temps-ecoule
    // Appelé automatiquement par le JavaScript quand le timer arrive à 0
    // Ce cas gère : personne n'a soumis OU seulement 1 joueur a soumis
    @PostMapping("/api/defis/temps-ecoule")
    @ResponseBody
    public Map<String, Object> tempsEcoule(
            @RequestParam Long defiId,
            HttpSession session) {

        // Chercher le défi
        Defi defi = defiRepository.findById(defiId).orElse(null);
        if (defi == null) return Map.of("erreur", "introuvable");

        // Vérifier que le défi n'est pas déjà terminé
        // (éviter de terminer deux fois si les 2 joueurs appellent cette route)
        if (defi.getStatut().equals("termine")) {
            return Map.of("message", "défi déjà terminé");
        }

        // Comparer ce qui a été soumis jusqu'ici
        // (même si personne ou seulement 1 joueur a soumis)
        String evalComparative = defiService.comparerDeuxSolutions(
            defi.getProbleme(),
            "(temps écoulé sans soumission)",
            defi.getJoueur1(),
            defi.getScoreJoueur1(), // 0 si pas soumis
            "(temps écoulé sans soumission)",
            defi.getJoueur2() != null ? defi.getJoueur2() : "?",
            defi.getScoreJoueur2(), // 0 si pas soumis
            defi.getTempsJoueur1(),
            defi.getTempsJoueur2()
        );

        // Déterminer le gagnant selon les scores actuels
        // Si personne n'a soumis → égalité
        // Si 1 joueur a soumis → il gagne automatiquement
        String gagnant = determinerGagnant(defi);
        defi.setGagnant(gagnant);
        defi.setStatut("termine");
        defiRepository.save(defi);

        // Donner +50 points au gagnant si applicable
        donnerPointsGagnant(gagnant);

        // Notifier les 2 joueurs que le temps est écoulé
        messagingTemplate.convertAndSend("/topic/defi/" + defiId,
            Map.of(
                "type",         "temps_ecoule", // ← Arrêt pour les 2
                "gagnant",      gagnant,
                "scoreJoueur1", defi.getScoreJoueur1(),
                "scoreJoueur2", defi.getScoreJoueur2(),
                "joueur1",      defi.getJoueur1(),
                "joueur2",      defi.getJoueur2() != null
                                ? defi.getJoueur2() : "?",
                "evaluation",   evalComparative,
                "raison",       "temps_ecoule" // Raison de la fin
            )
        );

        return Map.of(
            "gagnant", gagnant,
            "statut",  "termine",
            "raison",  "temps_ecoule"
        );
    }

    // ══════════════════════════════════════════════════
    // LISTE DES DÉFIS EN ATTENTE
    // ══════════════════════════════════════════════════

    // Route : GET /api/defis/attente
    // Retourne la liste des défis disponibles à rejoindre
    @GetMapping("/api/defis/attente")
    @ResponseBody
    public List<Map<String, Object>> getDefisAttente() {

        // Récupérer les défis avec statut "attente"
        List<Defi> defis =
            defiRepository.findByStatutOrderByIdDesc("attente");

        // Construire la liste simplifiée pour le JSON
        List<Map<String, Object>> liste = new ArrayList<>();
        for (Defi d : defis) {
            Map<String, Object> info = new HashMap<>();
            info.put("id",           d.getId());
            info.put("joueur1",      d.getJoueur1());
            info.put("niveau",       d.getNiveau());
            info.put("langage",      d.getLangage());
            info.put("dureeMinutes", d.getDureeMinutes());
            liste.add(info);
        }
        return liste;
    }

    // ══════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES PRIVÉES
    // ══════════════════════════════════════════════════

    // Donner +50 points au gagnant dans la base de données
    private void donnerPointsGagnant(String gagnant) {

        // Pas de points si égalité ou gagnant inconnu
        if (gagnant == null || gagnant.equals("egalite") || gagnant.isEmpty()) {
            return;
        }

        // Chercher le gagnant dans tous les utilisateurs
        List<User> tousUsers = userService.getAllUsers();
        for (User u : tousUsers) {
            if (u.getUsername().equals(gagnant)) {
                // Ajouter 50 points au gagnant
                u.setPoints(u.getPoints() + 50);
                userService.saveUser(u);
                break; // Arrêter la boucle une fois trouvé
            }
        }
    }

    // Déterminer le gagnant selon les scores et les temps
    private String determinerGagnant(Defi defi) {
        int score1 = defi.getScoreJoueur1(); // Score joueur 1
        int score2 = defi.getScoreJoueur2(); // Score joueur 2

        // Celui avec le score le plus élevé gagne
        if (score1 > score2) return defi.getJoueur1();
        if (score2 > score1) return defi.getJoueur2() != null
                                    ? defi.getJoueur2()
                                    : defi.getJoueur1();

        // Scores égaux → le plus rapide gagne
        if (defi.getTempsJoueur1() > 0 && defi.getTempsJoueur2() > 0) {
            return defi.getTempsJoueur1() < defi.getTempsJoueur2()
                ? defi.getJoueur1()  // Joueur 1 plus rapide
                : defi.getJoueur2(); // Joueur 2 plus rapide
        }

        // Si un seul joueur a soumis → il gagne
        if (defi.getTempsJoueur1() > 0 && defi.getTempsJoueur2() == 0) {
            return defi.getJoueur1();
        }
        if (defi.getTempsJoueur2() > 0 && defi.getTempsJoueur1() == 0) {
            return defi.getJoueur2() != null ? defi.getJoueur2() : "egalite";
        }

        // Personne n'a soumis → égalité
        return "egalite";
    }
}