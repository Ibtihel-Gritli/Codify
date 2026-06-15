package com.codify.app.controller;

import com.codify.app.model.User;

import com.codify.app.service.ExerciceService;
import com.codify.app.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.codify.app.model.HistoriqueExercice;
import com.codify.app.repository.HistoriqueRepository;
import java.util.List;

@Controller
public class ExerciceController {

    @Autowired
    private ExerciceService exerciceService;

    @Autowired
    private UserService userService;
    @Autowired
    private HistoriqueRepository historiqueRepository;

    // Page exercices
    @GetMapping("/exercices")
    public String exercices(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        model.addAttribute("niveau", user.getLevel());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("points", user.getPoints());
        model.addAttribute("exercicesCompletes", user.getExercicesCompletes());
        model.addAttribute("testDisponible", user.isTestDisponible());
        return "exercices";
    }

    // Générer 6 exercices IA selon niveau + langage
    @PostMapping("/api/exercices/generer")
    @ResponseBody
    public String genererExercices(
            @RequestParam String niveau,
            @RequestParam String langage,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "non connecté";
        return exerciceService.genererSerie(niveau, langage);
    }

    // Corriger + calculer complexité + mettre à jour stats
    @PostMapping("/api/exercices/corriger")
    @ResponseBody
    public Map<String, Object> corrigerReponse(
            @RequestParam String exercice,
            @RequestParam String reponse,
            @RequestParam String niveau,
            @RequestParam String langage,
            @RequestParam String titreExercice,
            @RequestParam(defaultValue = "0") long tempsSecondes,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Correction par l'IA
        String correction = exerciceService.corrigerReponse(
            exercice, reponse, niveau, langage
        );

        // Déterminer résultat
        String resultat;
        int pointsGagnes;
        int pointsBase = getPointsBase(niveau);

        if (correction.toUpperCase().contains("INCORRECT")
         || correction.toUpperCase().contains("FAUX")) {
            resultat = "incorrect";
            pointsGagnes = 0; // Incorrect → 0 point
        } else if (correction.toUpperCase().contains("PARTIEL")
                || correction.toUpperCase().contains("LOGIQUE CORRECTE")) {
            resultat = "partiel";
            pointsGagnes = pointsBase / 2; // Logique correcte → moitié des points
        } else {
            resultat = "correct";
            // Bonus si rapide (moins de 2 minutes)
            pointsGagnes = tempsSecondes > 0 && tempsSecondes < 120
                ? (int)(pointsBase * 1.5)
                : pointsBase;
        }

        // Extraire complexité du texte
        String complexite = extraireComplexite(correction);

        // Formater le temps
        long min = tempsSecondes / 60;
        long sec = tempsSecondes % 60;
        String tempsFormate = min + "min " + sec + "s";

        // Sauvegarder dans l'historique
        HistoriqueExercice histo = new HistoriqueExercice();
        histo.setUserId(user.getId());
        histo.setTitreExercice(titreExercice);
        histo.setNiveau(niveau);
        histo.setPointsGagnes(pointsGagnes);
        histo.setTempsSecondes(tempsSecondes);
        histo.setTempsFormate(tempsFormate);
        histo.setComplexite(complexite);
        histo.setResultat(resultat);
        historiqueRepository.save(histo);

        // Mettre à jour l'utilisateur (seulement si correct ou partiel)
        if (pointsGagnes > 0) {
            user.setPoints(user.getPoints() + pointsGagnes);
        }
        user.setTempsTotal(user.getTempsTotal() + tempsSecondes);
        user.setExercicesCompletes(user.getExercicesCompletes() + 1);

        if (user.getExercicesCompletes() >= 20) {
            user.setTestDisponible(true);
        }

        userService.saveUser(user);
        session.setAttribute("user", user);

        return Map.of(
            "correction", correction,
            "resultat", resultat,
            "pointsGagnes", pointsGagnes,
            "complexite", complexite,
            "totalPoints", user.getPoints(),
            "exercicesCompletes", user.getExercicesCompletes(),
            "testDisponible", user.isTestDisponible()
        );
    }

    private int getPointsBase(String niveau) {
        switch (niveau.toLowerCase()) {
            case "debutant": case "débutant": return 10;
            case "intermediaire": case "intermédiaire": return 20;
            default: return 30;
        }
    }

    private String extraireComplexite(String texte) {
        if (texte.contains("O(1)")) return "O(1)";
        if (texte.contains("O(log n)")) return "O(log n)";
        if (texte.contains("O(n log n)")) return "O(n log n)";
        if (texte.contains("O(n²)") || texte.contains("O(n^2)")) return "O(n²)";
        if (texte.contains("O(n)")) return "O(n)";
        return "O(n)"; // par défaut
    }

    // Calculer points selon temps et niveau
    private int calculerPoints(long tempsSecondes, String niveau) {
        int basePoints;
        switch (niveau.toLowerCase()) {
            case "debutant": case "débutant": basePoints = 10; break;
            case "intermediaire": case "intermédiaire": basePoints = 20; break;
            default: basePoints = 30;
        }
        // Bonus si rapide (moins de 2 minutes)
        if (tempsSecondes > 0 && tempsSecondes < 120) {
            return (int)(basePoints * 1.5); // x1.5 si moins de 2 min
        }
        return basePoints;
    }

    // Générer le test chronométré (très difficile)
    @PostMapping("/api/exercices/test")
    @ResponseBody
    public Map<String, Object> genererTest(
            @RequestParam String niveau,
            @RequestParam String langage,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Vérifier si test disponible
        if (!user.isTestDisponible()) {
            return Map.of(
                "erreur", "Test non disponible",
                "message", "Complétez 20 exercices d'abord !"
            );
        }

        String test = exerciceService.genererTest(niveau, langage);
        // Durée du test : 30 min pour débutant, 20 min pour intermédiaire, 15 pour avancé
        int dureeMinutes = niveau.contains("avance") ? 15 : niveau.contains("inter") ? 20 : 30;

        return Map.of(
            "test", test,
            "dureeMinutes", dureeMinutes
        );
    }

    // Évaluer le test
    @PostMapping("/api/exercices/evaluer")
    @ResponseBody
    public Map<String, Object> evaluerTest(
            @RequestParam String test,
            @RequestParam String reponse,
            @RequestParam String niveau,
            @RequestParam(defaultValue = "false") boolean tempsEcoule,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) return Map.of("erreur", "non connecté");

        // Si temps écoulé → automatiquement échoué
        if (tempsEcoule) {
            // Remettre le compteur à 0 pour refaire 20 exercices
            user.setExercicesCompletes(0);
            user.setTestDisponible(false);
            userService.saveUser(user);
            session.setAttribute("user", user);

            return Map.of(
                "reussi", false,
                "evaluation", "⏰ Temps écoulé ! Vous devez refaire 20 exercices.",
                "niveau", niveau
            );
        }

        String evaluation = exerciceService.evaluerTest(test, reponse, niveau);
        boolean reussi = evaluation.toUpperCase().contains("RÉUSSI")
                      || evaluation.toUpperCase().contains("REUSSI")
                      || evaluation.toUpperCase().contains("FÉLICITATIONS");

        if (reussi) {
            // Niveau amélioré, aucun point gagné, compteur remis à 0
            String nouveauNiveau = ameliorerNiveau(niveau);
            user.setLevel(nouveauNiveau);
            user.setExercicesCompletes(0);
            user.setTestDisponible(false);
            userService.saveUser(user);
            session.setAttribute("user", user);

            return Map.of(
                "reussi", true,
                "evaluation", evaluation,
                "nouveauNiveau", nouveauNiveau
            );
        } else {
            // Échoué : remettre compteur à 0, refaire 20 exercices
            user.setExercicesCompletes(0);
            user.setTestDisponible(false);
            userService.saveUser(user);
            session.setAttribute("user", user);

            return Map.of(
                "reussi", false,
                "evaluation", evaluation,
                "niveau", niveau
            );
        }
    }

    // Classement par niveau
    @GetMapping("/api/classement")
    @ResponseBody
    public List<Map<String, Object>> getClassement(
            @RequestParam String niveau,
            HttpSession session) {
        return userService.getClassementParNiveau(niveau);
    }

    private String ameliorerNiveau(String niveau) {
        switch (niveau.toLowerCase()) {
            case "debutant": case "débutant": return "intermediaire";
            case "intermediaire": case "intermédiaire": return "avance";
            default: return niveau;
        }
    }
}